/*
 * Copyright 2018 the Justify authors.
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

package org.leadpony.justify.internal.schema;

import javax.json.stream.JsonGenerator;

import org.leadpony.justify.core.Evaluator;
import org.leadpony.justify.core.InstanceType;
import org.leadpony.justify.core.JsonSchema;
import org.leadpony.justify.internal.evaluator.ConditionalEvaluator;
import org.leadpony.justify.internal.evaluator.Evaluators;

/**
 * A set of schemas representing "if", "then", and "else" keywords.
 *  
 * @author leadpony
 */
public class IfThenElse extends AbstractJsonSchema {
    
    private final JsonSchema ifSchema;
    private final JsonSchema thenSchema;
    private final JsonSchema elseSchema;
    
    public IfThenElse(JsonSchema ifSchema, JsonSchema thenSchema, JsonSchema elseSchema) {
        this.ifSchema = ifSchema;
        this.thenSchema = thenSchema;
        this.elseSchema = elseSchema;
    }

    @Override
    public Evaluator createEvaluator(InstanceType type) {
        if (ifSchema == null || (thenSchema == null && elseSchema == null)) {
            return Evaluators.ALWAYS_IGNORED;
        }
        Evaluator ifEvaluator = ifSchema.createEvaluator(type);
        Evaluator thenEvaluator = createEvaluator(thenSchema, type);
        Evaluator elseEvaluator = createEvaluator(elseSchema, type);
        return new ConditionalEvaluator(ifEvaluator, thenEvaluator, elseEvaluator);
    }

    @Override
    public void toJson(JsonGenerator generator) {
        if (ifSchema != null) {
            generator.writeKey("if");
            ifSchema.toJson(generator);
        }
        if (thenSchema != null) {
            generator.writeKey("then");
            thenSchema.toJson(generator);
        }
        if (elseSchema != null) {
            generator.writeKey("else");
            elseSchema.toJson(generator);
        }
    }

    @Override
    protected AbstractJsonSchema createNegatedSchema() {
        throw new UnsupportedOperationException();
    }
    
    private static Evaluator createEvaluator(JsonSchema schema, InstanceType type) {
        return (schema != null) ? schema.createEvaluator(type) : null;
    }
}
