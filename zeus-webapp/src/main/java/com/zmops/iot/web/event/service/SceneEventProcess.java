package com.zmops.iot.web.event.service;

import com.alibaba.fastjson.JSON;
import com.dtflys.forest.Forest;
import com.zmops.iot.async.executor.Async;
import com.zmops.iot.async.wrapper.WorkerWrapper;
import com.zmops.iot.domain.product.ProductEventService;
import com.zmops.iot.domain.product.ProductServiceParam;
import com.zmops.iot.domain.product.query.QProductEventService;
import com.zmops.iot.util.DefinitionsUtil;
import com.zmops.iot.util.ToolUtil;
import com.zmops.iot.web.device.service.work.DeviceServiceLogWorker;
import com.zmops.iot.web.device.service.work.ScenesLogWorker;
import com.zmops.iot.web.event.EventProcess;
import com.zmops.iot.web.event.dto.EventDataDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author yefei
 **/
@Slf4j
@Component
public class SceneEventProcess implements EventProcess {

    @Autowired
    DeviceServiceLogWorker deviceServiceLogWorker;

    @Autowired
    ScenesLogWorker scenesLogWorker;

    @Override
    public void process(EventDataDto eventData) {
        log.debug("--------scene event----------{}", eventData.getObjectid());
        Map<String, Object> alarmInfo = new ConcurrentHashMap<>(3);

        String productEventRuleId = eventData.getName();
        if (ToolUtil.isEmpty(productEventRuleId)) {
            return;
        }
        long eventRuleId = Long.parseLong(productEventRuleId);

        alarmInfo.put("eventRuleId", eventRuleId);

        WorkerWrapper<Map<String, Object>, Boolean> deviceServiceLogWork = WorkerWrapper.<Map<String, Object>, Boolean>builder().id("deviceServiceLogWorker")
                .worker(deviceServiceLogWorker).param(alarmInfo)
                .build();
        WorkerWrapper<Map<String, Object>, Boolean> scenesLogWork = WorkerWrapper.<Map<String, Object>, Boolean>builder().id("scenesLogWorker")
                .worker(scenesLogWorker).param(alarmInfo)
                .build();

        try {
            Async.work(1000, deviceServiceLogWork, scenesLogWork).awaitFinish();
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<ProductEventService> productEventServiceList = new QProductEventService().eventRuleId.eq(eventRuleId)
                .deviceId.isNull().findList();
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, List<ProductEventService>> collect = productEventServiceList.parallelStream().collect(Collectors.groupingBy(ProductEventService::getExecuteDeviceId));

        collect.forEach((key, value) -> {
            Map<String, Object> map = new ConcurrentHashMap<>();
            map.put("device", key);

            List<Map<String, Object>> serviceList = new ArrayList<>();
            value.forEach(val -> {
                Map<String, Object> serviceMap = new ConcurrentHashMap<>();
                serviceMap.put("name", DefinitionsUtil.getServiceName(val.getServiceId()));

                List<ProductServiceParam> paramList = DefinitionsUtil.getServiceParam(val.getServiceId());
                if (ToolUtil.isNotEmpty(paramList)) {
                    serviceMap.put("param", paramList.parallelStream().collect(Collectors.toMap(ProductServiceParam::getKey, ProductServiceParam::getValue)));
                }
                serviceList.add(serviceMap);
            });
            map.put("service", serviceList);
            list.add(map);
        });

        Forest.post("/device/action/exec").host("127.0.0.1").port(12800).contentTypeJson().addBody(JSON.toJSON(list)).execute();
    }

    @Override
    public boolean checkTag(String tag) {
        return "__scene__".equals(tag);
    }

}