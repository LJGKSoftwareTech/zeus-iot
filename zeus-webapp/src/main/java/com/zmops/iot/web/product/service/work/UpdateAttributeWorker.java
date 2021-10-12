package com.zmops.iot.web.product.service.work;


import com.zmops.iot.async.callback.IWorker;
import com.zmops.iot.async.wrapper.WorkerWrapper;
import com.zmops.iot.domain.product.ProductAttribute;
import com.zmops.iot.domain.product.query.QProductAttribute;
import com.zmops.iot.util.ToolUtil;
import com.zmops.iot.web.device.dto.DeviceDto;
import com.zmops.iot.web.product.dto.ProductAttr;
import io.ebean.DB;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author yefei
 * <p>
 * 产品属性修改 同步到设备 步骤
 */
@Slf4j
@Component
public class UpdateAttributeWorker implements IWorker<ProductAttr, Boolean> {

    private static final String ATTR_SOURCE_DEPEND = "18";

    @Override
    public Boolean action(ProductAttr productAttr, Map<String, WorkerWrapper<?, ?>> map) {
        log.debug("处理产品属性修改 同步到设备工作…………");

        Long attrId = productAttr.getAttrId();

        List<ProductAttribute> list = new QProductAttribute().templateId.eq(attrId).findList();

        //处理依赖属性
        Map<String, Long> attrIdMap = new ConcurrentHashMap<>(list.size());
        if (ATTR_SOURCE_DEPEND.equals(productAttr.getSource())) {
            List<ProductAttribute> productAttributeList = new QProductAttribute().templateId.eq(productAttr.getDepAttrId()).findList();
            attrIdMap = productAttributeList.parallelStream().collect(Collectors.toMap(ProductAttribute::getProductId, ProductAttribute::getAttrId));
        }

        List<ProductAttribute> newList = new ArrayList<>();
        for (ProductAttribute productAttribute : list) {
            ProductAttribute newProductAttribute = new ProductAttribute();
            ToolUtil.copyProperties(productAttribute, newProductAttribute);
            newProductAttribute.setName(productAttr.getAttrName());
            newProductAttribute.setKey(productAttr.getKey());
            newProductAttribute.setUnits(productAttr.getUnits());
            newProductAttribute.setSource(productAttr.getSource());
            newProductAttribute.setValueType(productAttr.getValueType());
            newProductAttribute.setValuemapid(productAttr.getValuemapid());
            if (ATTR_SOURCE_DEPEND.equals(productAttr.getSource()) && null != attrIdMap.get(productAttribute.getProductId())) {
                newProductAttribute.setDepAttrId(attrIdMap.get(productAttribute.getProductId()));
            }
            newList.add(newProductAttribute);
        }
        DB.updateAll(newList);

        return true;
    }


    @Override
    public Boolean defaultValue() {
        return true;
    }

}
