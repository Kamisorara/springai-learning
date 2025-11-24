package com.example.springaigraphdemo1.graphNode;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.example.springaigraphdemo1.model.ProductInfo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
@Component
public class MergeNode implements NodeAction {


    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        ProductInfo productInfo = new ProductInfo();
        productInfo.setSlogan((String) state.value("slogan").orElse(""));
        productInfo.setMaterial((String) state.value("material").orElse(""));
        productInfo.setColors((List<String>) state.value("colors").orElse(List.of()));
        productInfo.setSeason((String) state.value("season").orElse(""));
        return Map.of("productInfo", productInfo);
    }
}
