package com.quarks.android.Utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.text.Layout;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * Created by trivalent on 7/1/2016.
 */

public class MessageBubbleLayout extends FrameLayout {
    //Rect lastLineSpec = new Rect();

    private static final String TAG = " CHAT_BUBBLE_LAYOUT :: ";
    public MessageBubbleLayout(Context context) {
        super(context);

    }

    public MessageBubbleLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MessageBubbleLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(21)
    public MessageBubbleLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        TextView childTextView = (TextView) getChildAt(0);
        View childDateView = getChildAt(1);

        int view_width = MeasureSpec.getSize(widthMeasureSpec);

        int maxWidth = childTextView.getMaxWidth();

        int lineCount = childTextView.getLineCount();

        int dateViewHeight = childDateView.getMeasuredHeight();

        int dateViewWidth = childDateView.getMeasuredWidth();

        int textViewPadding = childTextView.getPaddingLeft() + childTextView.getPaddingRight();

        int lastLineStart = childTextView.getLayout().getLineStart(lineCount - 1);
        int lastLineEnd = childTextView.getLayout().getLineEnd(lineCount - 1);

        int lastLineWidth = (int)Layout.getDesiredWidth(childTextView.getText().subSequence(lastLineStart,
                lastLineEnd), childTextView.getPaint());

        int finalFramelayoutWidth = 0;
        int finalFrameLayoutHeight = 0;
        int viewPaddingLeftNRight = getPaddingLeft() + getPaddingRight();
        int finalFrameLayoutRequiredWidth = lastLineWidth + textViewPadding + dateViewWidth  + viewPaddingLeftNRight;

        int lineHeight = (childTextView.getMeasuredHeight() / lineCount) / 2;
        int bottomMargin = lineHeight - (dateViewHeight / 2);

        if(( (childTextView.getMeasuredWidth() + viewPaddingLeftNRight) >= maxWidth) || (finalFrameLayoutRequiredWidth >= maxWidth) ) {
            finalFramelayoutWidth = maxWidth;
            finalFrameLayoutHeight = childTextView.getMeasuredHeight() +  (dateViewHeight / 4);
            if((finalFrameLayoutRequiredWidth >= maxWidth) ) {
                finalFrameLayoutHeight += dateViewHeight  -  (dateViewHeight / 10);
                finalFramelayoutWidth = childTextView.getMeasuredWidth() + viewPaddingLeftNRight;
                ((LayoutParams)childDateView.getLayoutParams()).bottomMargin = 0;
            } else {
                ((LayoutParams)childDateView.getLayoutParams()).bottomMargin = bottomMargin;
            }
        } else {
            finalFramelayoutWidth = Math.max(finalFrameLayoutRequiredWidth, childTextView.getMeasuredWidth() + viewPaddingLeftNRight);
            finalFrameLayoutHeight = (int) (childTextView.getMeasuredHeight() +  (dateViewHeight / 4));
            ((LayoutParams)childDateView.getLayoutParams()).bottomMargin = bottomMargin;
        }
        if(finalFramelayoutWidth > maxWidth){
            finalFramelayoutWidth = maxWidth;
        }

        setMeasuredDimension(finalFramelayoutWidth, finalFrameLayoutHeight);
    }
}