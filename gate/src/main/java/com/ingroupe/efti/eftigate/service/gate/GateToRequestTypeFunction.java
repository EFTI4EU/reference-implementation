package com.ingroupe.efti.eftigate.service.gate;

import com.ingroupe.efti.commons.enums.RequestTypeEnum;
import com.ingroupe.efti.eftigate.config.GateProperties;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

@Slf4j
@Component
@AllArgsConstructor
public class GateToRequestTypeFunction implements Function<List<String>, RequestTypeEnum> {
    private final GateProperties gateProperties;

    @Override
    public RequestTypeEnum apply(final List<String> gatesUrls) {
        if (CollectionUtils.isNotEmpty(gatesUrls)) {
            if (gatesUrls.size() == 1 && gateProperties.isCurrentGate(gatesUrls.get(0))) {
                return RequestTypeEnum.LOCAL_METADATA_SEARCH;
            } else {
                return RequestTypeEnum.EXTERNAL_METADATA_SEARCH;
            }
        }
        return RequestTypeEnum.LOCAL_METADATA_SEARCH;
    }
}
