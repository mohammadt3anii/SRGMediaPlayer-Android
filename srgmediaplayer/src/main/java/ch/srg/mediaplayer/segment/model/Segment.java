package ch.srg.mediaplayer.segment.model;

import android.support.annotation.NonNull;

/**
 * Copyright (c) SRG SSR. All rights reserved.
 * <p>
 * License information is available from the LICENSE file.
 */
public class Segment implements Comparable<Segment> {
    private String identifier;
    private String title;
    private String description;
    private String imageUrl;
    private String blockingReason;
    private long markIn;
    private long markOut;
    private long duration;
    private int progress;
    private boolean displayable;
    private boolean isLive;
    private boolean is360;

    public Segment(String identifier, String title, String description, String imageUrl,
                   String blockingReason, long markIn, long markOut, long duration,
                   boolean displayable, boolean isLive, boolean is360) {
        this.identifier = identifier;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.blockingReason = blockingReason;
        this.markIn = markIn;
        this.markOut = markOut;
        this.duration = duration;
        this.displayable = displayable;
        this.isLive = isLive;
        this.is360 = is360;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public long getMarkIn() {
        return markIn;
    }

    public long getMarkOut() {
        return markOut;
    }

    public long getDuration() {
        return duration;
    }

    public void setProgress(int value) {
        progress = value;
    }

    public int getProgress() {
        return progress;
    }

    public String getBlockingReason() {
        return blockingReason;
    }

    public boolean isBlocked() {
        return blockingReason != null;
    }

    public String getIdentifier() {
        return identifier;
    }

    public boolean isDisplayable() {
        return displayable;
    }


    public boolean isLive() {
        return isLive;
    }

    public boolean is360() {
        return is360;
    }

    @Override
    public int compareTo(@NonNull Segment another) {
        return ((int) (markIn - another.getMarkIn()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Segment segment = (Segment) o;

        if (markIn != segment.markIn) return false;
        if (markOut != segment.markOut) return false;
        if (duration != segment.duration) return false;
        if (progress != segment.progress) return false;
        if (displayable != segment.displayable) return false;
        if (isLive != segment.isLive) return false;
        if (is360 != segment.is360) return false;
        if (identifier != null ? !identifier.equals(segment.identifier) : segment.identifier != null)
            return false;
        if (title != null ? !title.equals(segment.title) : segment.title != null) return false;
        if (description != null ? !description.equals(segment.description) : segment.description != null)
            return false;
        if (imageUrl != null ? !imageUrl.equals(segment.imageUrl) : segment.imageUrl != null)
            return false;
        return blockingReason != null ? blockingReason.equals(segment.blockingReason) : segment.blockingReason == null;
    }

    @Override
    public int hashCode() {
        int result = identifier != null ? identifier.hashCode() : 0;
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (imageUrl != null ? imageUrl.hashCode() : 0);
        result = 31 * result + (blockingReason != null ? blockingReason.hashCode() : 0);
        result = 31 * result + (int) (markIn ^ (markIn >>> 32));
        result = 31 * result + (int) (markOut ^ (markOut >>> 32));
        result = 31 * result + (int) (duration ^ (duration >>> 32));
        result = 31 * result + progress;
        result = 31 * result + (displayable ? 1 : 0);
        result = 31 * result + (isLive ? 1 : 0);
        result = 31 * result + (is360 ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Segment{" +
                "identifier='" + identifier + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", blockingReason='" + blockingReason + '\'' +
                ", markIn=" + markIn +
                ", markOut=" + markOut +
                ", duration=" + duration +
                ", progress=" + progress +
                ", displayable=" + displayable +
                ", isLive=" + isLive +
                ", is360=" + is360 +
                '}';
    }

}
