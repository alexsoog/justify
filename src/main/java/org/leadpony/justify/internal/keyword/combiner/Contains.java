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

package org.leadpony.justify.internal.keyword.combiner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.json.spi.JsonProvider;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

import org.leadpony.justify.core.Evaluator;
import org.leadpony.justify.core.InstanceType;
import org.leadpony.justify.core.JsonSchema;
import org.leadpony.justify.core.Problem;
import org.leadpony.justify.internal.base.ParserEvents;
import org.leadpony.justify.internal.base.ProblemBuilder;
import org.leadpony.justify.internal.base.ProblemReporter;
import org.leadpony.justify.internal.evaluator.EvaluatorAppender;
import org.leadpony.justify.internal.keyword.Keyword;

/**
 * @author leadpony
 */
class Contains extends UnaryCombiner {
    
    private int minContains;
    private int maxContains;
    
    Contains(JsonSchema subschema) {
        super(subschema);
        this.minContains = 1;
        this.maxContains = -1;
    }

    @Override
    public String name() {
        return "contains";
    }

    @Override
    public void createEvaluator(InstanceType type, EvaluatorAppender appender, JsonProvider jsonProvider) {
        if (type == InstanceType.ARRAY && (minContains > 0 || hasMaxContains())) {
            appender.append(new RepeatingSubschemaEvaluator());
        }
    }

    @Override
    public void configure(Map<String, Keyword> others) {
        if (others.containsKey("maxContains")) {
            maxContains = (((MaxContains)others.get("maxContains")).value());
        }
        if (others.containsKey("minContains")) {
            minContains = ((MinContains)others.get("minContains")).value();
        }
    }
    
    private boolean hasMaxContains() {
        return maxContains >= 0;
    }
    
    private class RepeatingSubschemaEvaluator implements Evaluator, ProblemReporter {

        private int trueEvaluations;
        private Evaluator itemEvaluator;
        private List<Problem> problems = new ArrayList<>();
        private List<List<Problem>> accumulatedProblems = new ArrayList<>();
        
        @Override
        public Result evaluate(Event event, JsonParser parser, int depth, Reporter reporter) {
            if (itemEvaluator == null) {
                itemEvaluator = createSubschemaEvaluator(event, parser, depth);
            }
            if (itemEvaluator != null) {
                evaluateSubschema(event, parser, depth);
            }
            if (depth == 0 && event == Event.END_ARRAY) {
                if (trueEvaluations < minContains) {
                    reportTooFewValid(parser, reporter);
                    return Result.FALSE;
                } else if (hasMaxContains() && trueEvaluations > maxContains) {
                    reportTooManyValid(parser, reporter);
                    return Result.FALSE;
                }
                return Result.TRUE;
            }
            return Result.PENDING;
        }
        
        private Evaluator createSubschemaEvaluator(Event event, JsonParser parser, int depth) {
            if (depth == 1 && ParserEvents.isValue(event)) {
                InstanceType type = ParserEvents.toInstanceType(event, parser);
                return getSubschema().createEvaluator(type);
            } else {
                return null;
            }
        }
        
        private void evaluateSubschema(Event event, JsonParser parser, int depth) {
            Result result = itemEvaluator.evaluate(event, parser, depth - 1, this);
            if (result != Result.PENDING) {
                itemEvaluator = null;
                if (result == Result.TRUE) {
                    ++trueEvaluations;
                } else if (result == Result.FALSE) {
                    accumulatedProblems.add(new ArrayList<Problem>(problems));
                    problems.clear();
                }
            }
        }
        
        private void reportTooFewValid(JsonParser parser, Reporter reporter) {
            ProblemBuilder builder = ProblemBuilder.newBuilder(parser);
            builder.withMessage("instance.problem.min.contains")
                   .withParameter("expected", minContains)
                   .withParameter("actual", trueEvaluations);
            accumulatedProblems.forEach(builder::withSubproblems);
            reporter.reportProblem(builder.build());
        }

        private void reportTooManyValid(JsonParser parser, Reporter reporter) {
            ProblemBuilder builder = ProblemBuilder.newBuilder(parser);
            builder.withMessage("instance.problem.max.contains")
                   .withParameter("expected", maxContains)
                   .withParameter("actual", trueEvaluations);
            reporter.reportProblem(builder.build());
        }

        @Override
        public void reportProblem(Problem problem) {
            problems.add(problem);
        }
    }
}