/*
 * Copyright (C) 2014 ohmage
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

package org.ohmage.condition.comparator;

import org.ohmage.condition.Fragment;
import org.ohmage.condition.InvalidArgumentException;
import org.ohmage.condition.terminal.Numeric;
import org.ohmage.condition.terminal.PromptId;
import org.ohmage.condition.terminal.Terminal;
import org.ohmage.prompts.NumberPrompt;
import org.ohmage.prompts.Prompt;

import java.util.Map;

/**
 * <p>
 * A {@link Comparator} that returns true when a left {@link Terminal} is
 * greater than or equal to a right {@link Terminal}. Both {@link Terminal}s
 * must be either be a {@link PromptId} that references a number prompt or
 * {@link Numeric}.
 * </p>
 *
 * @author John Jenkins
 */
public class GreaterThanEquals extends Comparator {
    /**
     * <p>
     * A builder for {@link org.ohmage.condition.comparator.GreaterThanEquals} objects.
     * </p>
     *
     * @author John Jenkins
     */
    public static class Builder
        implements Comparator.Builder<GreaterThanEquals> {

        /**
         * The builder for the left operand.
         */
        private Terminal.Builder<?> left;
        /**
         * The builder for the right operand.
         */
        private Terminal.Builder<?> right;

        /*
         * (non-Javadoc)
         * @see org.ohmage.domain.survey.condition.Condition.Fragment.Builder#merge(org.ohmage.domain.survey.condition.Condition.Fragment.Builder)
         */
        @Override
        public Fragment.Builder<?> merge(final Fragment.Builder<?> other) {
            if(other instanceof Terminal.Builder<?>) {
                if(left == null) {
                    left = (Terminal.Builder<?>) other;
                }
                else if(right == null) {
                    right = (Terminal.Builder<?>) other;
                }
                else {
                    throw
                        new InvalidArgumentException(
                            "Multiple terminals are in sequence.");
                }

                return this;
            }
            else if(other instanceof Comparator.Builder<?>) {
                throw
                    new InvalidArgumentException(
                        "A comparator cannot be compared to another " +
                            "comparator.");
            }
            else {
                return other.merge(this);
            }
        }

        /*
         * (non-Javadoc)
         * @see org.ohmage.domain.survey.condition.Condition.Fragment.Builder#build()
         */
        @Override
        public GreaterThanEquals build() throws InvalidArgumentException {
            if(left == null) {
                throw
                    new InvalidArgumentException(
                        "The 'greater than or equals' does not have a left " +
                            "operand.");
            }
            if(right == null) {
                throw
                    new InvalidArgumentException(
                        "The 'greater than or equals' does not have a right " +
                            "operand.");
            }

            return new GreaterThanEquals(left.build(), right.build());
        }
    }

    /**
     * The string value of an {@link org.ohmage.condition.comparator.GreaterThanEquals} within a condition
     * sentence.
     */
    public static final String VALUE = ">=";

    /**
     * The left operand.
     */
    private final Terminal left;
    /**
     * The right operand.
     */
    private final Terminal right;

    /**
     * Creates a new LessThanEquals object with left and right operands.
     *
     * @param left
     *        The left operand.
     *
     * @param right
     *        The right operand.
     *
     * @throws InvalidArgumentException
     *         One or both of the operands is null.
     */
    public GreaterThanEquals(final Fragment left, final Fragment right)
        throws InvalidArgumentException {

        if(left == null) {
            throw
                new InvalidArgumentException(
                    "The left operand is missing.");
        }
        if(! (left instanceof Terminal)) {
            throw
            new InvalidArgumentException(
                "The left operand is not a terminal value.");
        }
        if(right == null) {
            throw
                new InvalidArgumentException(
                    "The right operand is missing.");
        }
        if(! (right instanceof Terminal)) {
            throw
            new InvalidArgumentException(
                "The right operand is not a terminal value.");
        }

        this.left = (Terminal) left;
        this.right = (Terminal) right;
    }

    /*
     * (non-Javadoc)
     * @see org.ohmage.domain.survey.condition.Fragment#validate(java.util.Map)
     */
    @Override
    public void validate(final Map<String, Prompt> surveyItems)
        throws InvalidArgumentException {

        left.validate(surveyItems);
        right.validate(surveyItems);

        boolean fail = false;
        if(! (left instanceof Numeric)) {
            if(left instanceof PromptId) {
                Prompt surveyItem =
                    surveyItems.get(((PromptId) left).getPromptId());

                if(! (surveyItem instanceof NumberPrompt)) {
                    fail = true;
                }
            }
            else {
                fail = true;
            }
        }
        if(! (right instanceof Numeric)) {
            if(right instanceof PromptId) {
                Prompt surveyItem =
                    surveyItems.get(((PromptId) right).getPromptId());

                if(! (surveyItem instanceof NumberPrompt)) {
                    fail = true;
                }
            }
            else {
                fail = true;
            }
        }

        if(fail) {
            throw
                new InvalidArgumentException(
                    "The 'greater than or equals' operator may only be used " +
                        "with number prompts and numbers.");
        }
    }

    /*
     * (non-Javadoc)
     * @see org.ohmage.domain.survey.condition.Fragment#evaluate(java.util.Map)
     */
    @Override
    public boolean evaluate(final Map<String, Object> responses) {
        Object rightValue = right.getValue(responses);

        return
            left.greaterThanValue(responses, rightValue) ||
            left.equalsValue(responses, rightValue);
    }
}