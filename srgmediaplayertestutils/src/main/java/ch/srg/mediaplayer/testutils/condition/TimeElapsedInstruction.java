package ch.srg.mediaplayer.testutils.condition;

/**
 * Copyright (c) SRG SSR. All rights reserved.
 * <p>
 * License information is available from the LICENSE file.
 */
public class TimeElapsedInstruction extends Instruction {

    private final long startTime;
    private long waitingTime;

    public TimeElapsedInstruction(long waitingTime){
        this.startTime = System.currentTimeMillis();
        this.waitingTime = waitingTime;
    }

    @Override
    public String getDescription() {
        return "Wait for :" + waitingTime;
    }

    @Override
    public boolean checkCondition() {
        long elapsed = System.currentTimeMillis() - startTime;
        return elapsed >= waitingTime;
    }
}
