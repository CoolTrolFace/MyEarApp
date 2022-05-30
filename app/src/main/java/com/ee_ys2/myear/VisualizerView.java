package com.ee_ys2.myear;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

public class VisualizerView extends View {

    private final int MAX_SIZE = 500;       // Maximum size that view can get
    private final int MAX_AMPLITUDE = 5000; // Maximum amplitude to calculate as max size

    /**
     * Visualizer view constructors for different version and different usages.
     * @param context
     */
    public VisualizerView(Context context) {
        super(context);

    }
    public VisualizerView(Context context, AttributeSet attributeSet) {
        super(context,attributeSet);
        init(attributeSet);

    }
    public VisualizerView(Context context, AttributeSet attributeSet,int defStyleAttr) {
        super(context,attributeSet,defStyleAttr);
        init(attributeSet);

    }
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public VisualizerView(Context context, AttributeSet attributeSet, int defStyleAttr, int defStyleRes) {
        super(context,attributeSet,defStyleAttr,defStyleRes);
        init(attributeSet);
    }
    private void init(@Nullable AttributeSet attr){

    }

    /**
     * Sets the size of visualizer view as amplitude gets higher
     * @param amplitude sound amplitude
     */
    public void setSize(int amplitude) {
        if(amplitude==0) return;
        ViewGroup.LayoutParams params = this.getLayoutParams();
        if(amplitude>MAX_AMPLITUDE) {
            amplitude=MAX_AMPLITUDE;
        }
        amplitude=(MAX_SIZE*amplitude)/MAX_AMPLITUDE;
        params.height = amplitude;
        params.width = amplitude;
        this.setLayoutParams(params);
    }

    /**
     * Resets the size of visualizer to default
     */
    public void reset(){
        ViewGroup.LayoutParams params = this.getLayoutParams();
        params.height = 0;
        params.width = 0;
        this.setLayoutParams(params);
    }
}
