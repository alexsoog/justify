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

import javax.json.JsonBuilderFactory;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

import org.leadpony.justify.api.Evaluator;
import org.leadpony.justify.api.InstanceType;
import org.leadpony.justify.api.JsonSchema;
import org.leadpony.justify.internal.base.ParserEvents;
import org.leadpony.justify.internal.evaluator.AbstractConjunctiveItemsEvaluator;
import org.leadpony.justify.internal.evaluator.AbstractDisjunctiveItemsEvaluator;
import org.leadpony.justify.internal.keyword.ArrayKeyword;

/**
 * Combiner representing "contains" keyword.
 * 
 * @author leadpony
 */
class Contains extends UnaryCombiner implements ArrayKeyword {
    
    private int min;
    
    Contains(JsonSchema subschema) {
        super(subschema);
        this.min = 1;
    }

    @Override
    public String name() {
        return "contains";
    }
    
    @Override
    protected Evaluator doCreateEvaluator(InstanceType type, JsonBuilderFactory builderFactory) {
        if (this.min == 1) {
            return createItemsEvaluator();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    protected Evaluator doCreateNegatedEvaluator(InstanceType type, JsonBuilderFactory builderFactory) {
        if (this.min == 1) {
            return createNegatedItemsEvaluator();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private Evaluator createItemsEvaluator() {
        final JsonSchema subschema = getSubschema();
        return new AbstractDisjunctiveItemsEvaluator(this) {
            @Override
            public void updateChildren(Event event, JsonParser parser) {
                if (ParserEvents.isValue(event)) {
                    InstanceType type = ParserEvents.toInstanceType(event, parser);
                    append(subschema.createEvaluator(type));
                }
            }
        };
    }

    private Evaluator createNegatedItemsEvaluator() {
        final JsonSchema subschema = getSubschema();
        return new AbstractConjunctiveItemsEvaluator(this) {
            @Override
            public void updateChildren(Event event, JsonParser parser) {
                if (ParserEvents.isValue(event)) {
                    InstanceType type = ParserEvents.toInstanceType(event, parser);
                    append(subschema.createNegatedEvaluator(type));
                }
            }
        };
    }
 }