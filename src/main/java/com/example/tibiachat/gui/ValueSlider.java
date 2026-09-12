package com.example.tibiachat.gui;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public class ValueSlider extends SliderWidget {
    private final double min;
    private final double max;
    private final boolean wholeNumber;
    private final DoubleFunction<Text> messageFactory;
    private final DoubleConsumer onChange;

    public ValueSlider(int x, int y, int width, int height, double min, double max,
                        double initialValue, boolean wholeNumber,
                        DoubleFunction<Text> messageFactory, DoubleConsumer onChange) {
        super(x, y, width, height, Text.empty(), normalize(initialValue, min, max));
        this.min = min;
        this.max = max;
        this.wholeNumber = wholeNumber;
        this.messageFactory = messageFactory;
        this.onChange = onChange;
        updateMessage();
    }

    private static double normalize(double v, double min, double max) {
        if (max <= min) return 0.0;
        return Math.max(0.0, Math.min(1.0, (v - min) / (max - min)));
    }

    public double getRealValue() {
        double v = min + (max - min) * this.value;
        return wholeNumber ? Math.round(v) : v;
    }

    public void setRealValueSilently(double v) {
        this.value = normalize(v, min, max);
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        setMessage(messageFactory.apply(getRealValue()));
    }

    @Override
    protected void applyValue() {
        onChange.accept(getRealValue());
    }
}
