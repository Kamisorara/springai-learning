package com.example.springaigraphdemo2.graphNode;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.example.springaigraphdemo2.model.AnimalInfo;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MergeNode implements NodeAction {

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        AnimalInfo animalInfo = new AnimalInfo();
        // 合并 AnimalCount 结果
        String animalCountStr = (String) state.value("animalCount")
                .orElse("0");
        animalInfo.setMessage(animalCountStr);
        return Map.of("animalCount", animalCountStr);
    }
}
