/*
 * Copyright 2018-2019 the Justify authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.leadpony.justify.internal.keyword.combiner;

import java.util.List;
import java.util.Map;

import javax.json.JsonBuilderFactory;

import org.leadpony.justify.api.Evaluator;
import org.leadpony.justify.api.InstanceType;
import org.leadpony.justify.api.JsonSchema;
import org.leadpony.justify.internal.evaluator.ConditionalEvaluator;
import org.leadpony.justify.internal.keyword.Keyword;

/**
 * "If" conditional keyword.
 *
 * @author leadpony
 */
class If extends Conditional {

    private JsonSchema thenSchema;
    private JsonSchema elseSchema;

    If(JsonSchema schema) {
        super(schema);
    }

    @Override
    public String name() {
        return "if";
    }

    @Override
    protected Evaluator doCreateEvaluator(InstanceType type, JsonBuilderFactory builderFactory) {
        Evaluator ifEvaluator = getSubschema().createEvaluator(type);
        Evaluator thenEvaluator = thenSchema != null ? thenSchema.createEvaluator(type)
                : JsonSchema.TRUE.createEvaluator(type);
        Evaluator elseEvaluator = elseSchema != null ? elseSchema.createEvaluator(type)
                : JsonSchema.TRUE.createEvaluator(type);
        return new ConditionalEvaluator(ifEvaluator, thenEvaluator, elseEvaluator);
    }

    @Override
    protected Evaluator doCreateNegatedEvaluator(InstanceType type, JsonBuilderFactory builderFactory) {
        Evaluator ifEvaluator = getSubschema().createEvaluator(type);
        Evaluator thenEvaluator = thenSchema != null ? thenSchema.createNegatedEvaluator(type)
                : getSubschema().createNegatedEvaluator(type);
        Evaluator elseEvaluator = elseSchema != null ? elseSchema.createNegatedEvaluator(type)
                : getSubschema().createEvaluator(type);
        return new ConditionalEvaluator(ifEvaluator, thenEvaluator, elseEvaluator);
    }

    @Override
    public void addToEvaluatables(List<Keyword> evaluatables, Map<String, Keyword> keywords) {
        if (keywords.containsKey("then")) {
            thenSchema = ((UnaryCombiner) keywords.get("then")).getSubschema();
        }
        if (keywords.containsKey("else")) {
            elseSchema = ((UnaryCombiner) keywords.get("else")).getSubschema();
        }
        if (thenSchema != null || elseSchema != null) {
            evaluatables.add(this);
        }
    }
}
