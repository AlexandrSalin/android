package org.ohmage.widget;

/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context;
import android.os.Handler;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.method.NumberKeyListener;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import org.ohmage.app.R;

import java.math.BigDecimal;

/**
 * A view for selecting a number
 * <p/>
 * For a dialog using this view, see {@link android.app.TimePickerDialog}.
 *
 * @hide
 */
public class NumberPicker extends LinearLayout {

    /**
     * The callback interface used to indicate the number value has been adjusted.
     */
    public interface OnChangedListener {
        /**
         * @param picker The NumberPicker associated with this listener.
         * @param oldVal The previous value.
         * @param newVal The new value.
         */
        void onChanged(NumberPicker picker, BigDecimal oldVal, BigDecimal newVal);
    }

    /**
     * Interface used to format the number into a string for presentation
     */
    public interface Formatter {
        String toString(BigDecimal value);
    }

    /*
     * Use a custom NumberPicker formatting callback to use two-digit
     * minutes strings like "01".  Keeping a static formatter etc. is the
     * most efficient way to do this; it avoids creating temporary objects
     * on every call to format().
     */
    public static final NumberPicker.Formatter TWO_DIGIT_FORMATTER =
            new NumberPicker.Formatter() {
                final StringBuilder mBuilder = new StringBuilder();

                final java.util.Formatter mFmt = new java.util.Formatter(
                        mBuilder, java.util.Locale.US);

                final Object[] mArgs = new Object[1];

                @Override
                public String toString(BigDecimal value) {
                    mArgs[0] = value;
                    mBuilder.delete(0, mBuilder.length());
                    mFmt.format("%02d", mArgs);
                    return mFmt.toString();
                }
            };

    private final Handler mHandler;

    private final Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            validateInput(mText);
            updateCurrent(mIncrement, mDecrement);
            mHandler.postDelayed(this, mSpeed);
        }
    };

    private void updateCurrent(boolean increment, boolean decrement) {
        if (mCurrent == null) {
            mCurrent = new BigDecimal(0);
            changeCurrent(mCurrent);
        } else if (increment) {
            changeCurrent(mCurrent.add(new BigDecimal(1)));
        } else if (decrement) {
            changeCurrent(mCurrent.subtract(new BigDecimal(1)));
        }
    }

    private final EditText mText;

    private final InputFilter mNumberInputFilter;

    /**
     * Lower value of the range of numbers allowed for the NumberPicker
     */
    private BigDecimal mStart = BigDecimal.valueOf(Double.MAX_VALUE).negate();

    /**
     * Upper value of the range of numbers allowed for the NumberPicker
     */
    private BigDecimal mEnd = BigDecimal.valueOf(Double.MAX_VALUE);

    /**
     * Current value of this NumberPicker
     */
    private BigDecimal mCurrent;

    /**
     * Previous value of this NumberPicker.
     */
    private BigDecimal mPrevious;

    private OnChangedListener mListener;

    private Formatter mFormatter;

    private long mSpeed = 300;

    private boolean mIncrement;

    private boolean mDecrement;

    /**
     * If <code>true</code> decimal values are not allowed
     */
    private boolean mWholeNumbers;

    /**
     * Create a new number picker
     *
     * @param context the application environment
     */
    public NumberPicker(Context context) {
        this(context, null);
    }

    /**
     * Create a new number picker
     *
     * @param context the application environment
     * @param attrs   a collection of attributes
     */
    public NumberPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        LayoutInflater inflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_number_picker, this, true);
        mHandler = new Handler();

        OnClickListener clickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                validateInput(mText);
                if (!mText.hasFocus()) mText.requestFocus();

                // now perform the increment/decrement
                updateCurrent(R.id.increment == v.getId(), R.id.decrement == v.getId());
            }
        };

        OnLongClickListener longClickListener = new OnLongClickListener() {
            /**
             * We start the long click here but rely on the {@link NumberPickerButton}
             * to inform us when the long click has ended.
             */
            @Override
            public boolean onLongClick(View v) {
                /* The text view may still have focus so clear it's focus which will
                 * trigger the on focus changed and any typed values to be pulled.
                 */
                mText.clearFocus();

                if (R.id.increment == v.getId()) {
                    mIncrement = true;
                    mHandler.post(mRunnable);
                } else if (R.id.decrement == v.getId()) {
                    mDecrement = true;
                    mHandler.post(mRunnable);
                }
                return true;
            }
        };

        InputFilter inputFilter = new NumberPickerInputFilter();
        mNumberInputFilter = new NumberRangeKeyListener();
        mIncrementButton = (NumberPickerButton) findViewById(R.id.increment);
        mIncrementButton.setOnClickListener(clickListener);
        mIncrementButton.setOnLongClickListener(longClickListener);
        mIncrementButton.setNumberPicker(this);

        mDecrementButton = (NumberPickerButton) findViewById(R.id.decrement);
        mDecrementButton.setOnClickListener(clickListener);
        mDecrementButton.setOnLongClickListener(longClickListener);
        mDecrementButton.setNumberPicker(this);

        mText = (EditText) findViewById(R.id.timepicker_input);
        mText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {

            }

            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override public void afterTextChanged(Editable s) {
                validateInput(mText);
            }
        });
        mText.setFilters(new InputFilter[]{inputFilter});
        mText.setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED |
                              ((mWholeNumbers) ? 0 : InputType.TYPE_NUMBER_FLAG_DECIMAL));

        if (!isEnabled()) {
            setEnabled(false);
        }
    }

    /**
     * Set the enabled state of this view. The interpretation of the enabled
     * state varies by subclass.
     *
     * @param enabled True if this view is enabled, false otherwise.
     */
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mIncrementButton.setEnabled(enabled);
        mDecrementButton.setEnabled(enabled);
        mText.setEnabled(enabled);
    }

    /**
     * Set the callback that indicates the number has been adjusted by the user.
     *
     * @param listener the callback, should not be null.
     */
    public void setOnChangeListener(OnChangedListener listener) {
        mListener = listener;
    }

    /**
     * Set the formatter that will be used to format the number for presentation
     *
     * @param formatter the formatter object.  If formatter is null, String.valueOf()
     *                  will be used
     */
    public void setFormatter(Formatter formatter) {
        mFormatter = formatter;
    }

    /**
     * Set the range of numbers allowed for the number picker. The current
     * value will be automatically set to the start.
     *
     * @param start the start of the range (inclusive)
     * @param end   the end of the range (inclusive)
     */
    public void setRange(BigDecimal start, BigDecimal end) {
        mStart = start;
        if (mStart == null)
            mStart = BigDecimal.valueOf(Double.MAX_VALUE).negate();
        mEnd = end;
        if (mEnd == null)
            mEnd = BigDecimal.valueOf(Double.MAX_VALUE);
        //mCurrent = start;
        updateView();
    }

    /**
     * Set the current value for the number picker.
     *
     * @param current the current value the start of the range (inclusive)
     * @throws IllegalArgumentException when current is not within the range
     *                                  of of the number picker
     */
    public void setCurrent(BigDecimal current) {
        if (current != null) {
            if (current.compareTo(mStart) < 0 || current.compareTo(mEnd) > 0) {
                throw new IllegalArgumentException(
                        "current should be >= start and <= end");
            }
        }
        mCurrent = current;
        updateView();
    }

    /**
     * Set true if this number picker should only allow whole numbers
     *
     * @param wholeNumbers
     */
    public void setWholeNumbers(boolean wholeNumbers) {
        mWholeNumbers = wholeNumbers;
    }

    /**
     * Sets the speed at which the numbers will scroll when the +/-
     * buttons are longpressed
     *
     * @param speed The speed (in milliseconds) at which the numbers will scroll
     *              default 300ms
     */
    public void setSpeed(long speed) {
        mSpeed = speed;
    }

    private String formatNumber(BigDecimal value) {
        return (mFormatter != null)
                ? mFormatter.toString(value)
                : String.valueOf(value);
    }

    /**
     * Sets the current value of this NumberPicker, and sets mPrevious to the previous
     * value.  If current is greater than mEnd less than mStart, the value of mCurrent
     * is wrapped around.
     * <p/>
     * Subclasses can override this to change the wrapping behavior
     *
     * @param current the new value of the NumberPicker
     */
    protected void changeCurrent(BigDecimal current) {
        // Don't wrap around the values if we go past the start or end
        if(current != null) {
            if (current.compareTo(mEnd) > 0) {
                current = mEnd;
            } else if (current.compareTo(mStart) < 0) {
                current = mStart;
            }
        }
        mPrevious = mCurrent;
        mCurrent = current;
        notifyChange();
        updateView();
    }

    /**
     * Notifies the listener, if registered, of a change of the value of this
     * NumberPicker.
     */
    private void notifyChange() {
        if (mListener != null) {
            mListener.onChanged(this, mPrevious, mCurrent);
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        validateInput(mText);
    }

    /**
     * Updates the view of this NumberPicker.  If displayValues were specified
     * in {@link #setRange}, the string corresponding to the index specified by
     * the current value will be returned.
     */
    private void updateView() {
        mText.setText(formatNumber(mCurrent));
        mText.setSelection(mText.getText().length());
    }

    private boolean validateCurrentView(CharSequence str) {
        BigDecimal val = getSelectedPos(str.toString());
        if ((val.compareTo(mStart) >= 0) && (val.compareTo(mEnd) <= 0)) {
            if (!val.equals(mCurrent)) {
                changeCurrent(val);
            }
        }
        return (val.compareTo(mStart) >= 0) && (val.compareTo(mEnd) <= 0);
    }

    private boolean validateInput(View v) {
        String str = String.valueOf(((TextView) v).getText());
        if ("".equals(str)) {
            mPrevious = mCurrent;
            mCurrent = null;
            notifyChange();
            return true;
        } else {

            // Check the new value and ensure it's in range
            return validateCurrentView(str);
        }
    }

    /**
     * @hide
     */
    public void cancelIncrement() {
        mIncrement = false;
    }

    /**
     * @hide
     */
    public void cancelDecrement() {
        mDecrement = false;
    }

    private static final char[] DIGIT_CHARACTERS = new char[]{
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
    };

    private static final char[] DECIMAL_DIGIT_CHARACTERS = new char[]{
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.'
    };

    private NumberPickerButton mIncrementButton;

    private NumberPickerButton mDecrementButton;

    private class NumberPickerInputFilter implements InputFilter {
        @Override
        public CharSequence filter(CharSequence source, int start, int end,
                Spanned dest, int dstart, int dend) {
            return mNumberInputFilter.filter(source, start, end, dest, dstart, dend);
        }
    }

    private class NumberRangeKeyListener extends NumberKeyListener {

        // XXX This doesn't allow for range limits when controlled by a
        // soft input method!
        @Override
        public int getInputType() {
            return InputType.TYPE_CLASS_NUMBER;
        }

        @Override
        protected char[] getAcceptedChars() {
            if (mWholeNumbers)
                return DIGIT_CHARACTERS;
            return DECIMAL_DIGIT_CHARACTERS;
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end,
                Spanned dest, int dstart, int dend) {

            CharSequence filtered = super.filter(source, start, end, dest, dstart, dend);
            if (filtered == null) {
                filtered = source.subSequence(start, end);
            }

            // Remove .0 if that is all that is there
            if (filtered.toString().matches(".*\\.0$"))
                filtered = filtered.subSequence(0, filtered.length() - 2);

            // Add the negative sign back in to the front if it was there if we accept negative numbers
            if (mStart.signum() == -1 && source.length() > 0 && source.charAt(0) == '-')
                filtered = "-" + filtered;

            String result = String.valueOf(dest.subSequence(0, dstart))
                            + filtered
                            + dest.subSequence(dend, dest.length());

            if ("".equals(result)) {
                return result;
            }
            BigDecimal val = getSelectedPos(result);

            /* Ensure the user can't type in a value greater
             * than the max allowed. We have to allow less than min
             * as the user might want to delete some numbers
             * and then type a new number.
             */
            if (val.compareTo(mEnd) > 0) {
                return "";
            } else {
                return filtered;
            }
        }
    }

    private BigDecimal getSelectedPos(String str) {
        try {
            return new BigDecimal(str);
        } catch (NumberFormatException e) {
            /* Ignore as if it's not a number we don't care */
        }
        return mStart;
    }

    /**
     * Returns the current value of the NumberPicker
     *
     * @return the current value.
     */
    public double getCurrent() {
        return mCurrent.doubleValue();
    }

    /**
     * Returns the upper value of the range of the NumberPicker
     *
     * @return the uppper number of the range.
     */
    protected double getEndRange() {
        return mEnd.doubleValue();
    }

    /**
     * Returns the lower value of the range of the NumberPicker
     *
     * @return the lower number of the range.
     */
    protected double getBeginRange() {
        return mStart.doubleValue();
    }

    public boolean forceValidateInput() {
        return validateInput(mText);
    }

    public void setOnEditorActionListener(OnEditorActionListener onEditorActionListener) {
        mText.setOnEditorActionListener(onEditorActionListener);
    }

    public void setImeActionLabel(CharSequence label, int actionId) {
        mText.setImeActionLabel(label, actionId);
    }
}