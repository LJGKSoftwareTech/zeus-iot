package com.zmops.iot.web.protocol.service;

import com.zmops.iot.domain.protocol.ProtocolComponent;
import com.zmops.iot.domain.protocol.ProtocolService;
import com.zmops.iot.domain.protocol.query.QProtocolGateway;
import com.zmops.iot.domain.protocol.query.QProtocolService;
import com.zmops.iot.model.exception.ServiceException;
import com.zmops.iot.model.page.Pager;
import com.zmops.iot.util.ToolUtil;
import com.zmops.iot.web.exception.enums.BizExceptionEnum;
import com.zmops.iot.web.protocol.dto.ProtocolServiceDto;
import com.zmops.iot.web.protocol.dto.param.ProtocolServiceParam;
import io.ebean.DB;
import io.ebean.PagedList;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author yefei
 **/
@Service
public class ProtocolSvrService {

    public Pager<ProtocolServiceDto> getProtocolServiceByPage(ProtocolServiceParam protocolServiceParam) {
        QProtocolService qProtocolService = new QProtocolService();

        if (ToolUtil.isNotEmpty(protocolServiceParam.getName())) {
            qProtocolService.name.contains(protocolServiceParam.getName());
        }

        if (ToolUtil.isNotEmpty(protocolServiceParam.getProtocolType())) {
            qProtocolService.protocolType.eq(protocolServiceParam.getProtocolType());
        }

        PagedList<ProtocolService> pagedList = qProtocolService.setFirstRow((protocolServiceParam.getPage() - 1) * protocolServiceParam.getMaxRow())
                .setMaxRows(protocolServiceParam.getMaxRow()).findPagedList();

        List<ProtocolServiceDto> protocolComponentDtoList = ToolUtil.convertBean(pagedList.getList(), ProtocolServiceDto.class);

        return new Pager<>(protocolComponentDtoList, pagedList.getTotalCount());
    }

    public List<ProtocolService> list(ProtocolServiceParam protocolServiceParam) {
        QProtocolService qProtocolService = new QProtocolService();
        if (ToolUtil.isNotEmpty(protocolServiceParam.getName())) {
            qProtocolService.name.contains(protocolServiceParam.getName());
        }

        if (ToolUtil.isNotEmpty(protocolServiceParam.getProtocolType())) {
            qProtocolService.protocolType.eq(protocolServiceParam.getProtocolType());
        }
        return qProtocolService.findList();
    }

    public ProtocolService create(ProtocolServiceParam protocolServiceParam) {
        ProtocolService ProtocolService = new ProtocolService();
        ToolUtil.copyProperties(protocolServiceParam, ProtocolService);
        DB.insert(ProtocolService);
        return ProtocolService;
    }

    public ProtocolService update(ProtocolServiceParam protocolServiceParam) {
        ProtocolService ProtocolService = new ProtocolService();
        ToolUtil.copyProperties(protocolServiceParam, ProtocolService);
        DB.insert(ProtocolService);
        return ProtocolService;
    }

    public void delete(List<Long> protocolServiceIds) {
        int count = new QProtocolGateway().protocolServiceId.in(protocolServiceIds).findCount();
        if (count > 0) {
            throw new ServiceException(BizExceptionEnum.PROTOCOL_SERVICE_HAS_BIND_GATEWAY);
        }

        new QProtocolService().protocolServiceId.in(protocolServiceIds).delete();
    }


}
