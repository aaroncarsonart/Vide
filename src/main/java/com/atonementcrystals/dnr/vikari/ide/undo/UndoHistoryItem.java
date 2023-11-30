package com.atonementcrystals.dnr.vikari.ide.undo;

import java.util.LinkedList;

/**
 * Models one entry for the UndoHistory.
 */
public class UndoHistoryItem {
    private UndoHistoryItemType type;
    private int startCursor;
    private int endCursor;
    private LinkedList<Character> modifiedText;
    private boolean finished;

    public UndoHistoryItem(UndoHistoryItemType type, int startCursor, int endCursor, String edit) {
        this.type = type;
        this.startCursor = startCursor;
        this.endCursor = endCursor;
        this.modifiedText = new LinkedList<>();
        for (Character c : edit.toCharArray()) {
            this.modifiedText.add(c);
        }
        this.finished = false;
    }

    public void setStartCursor(int startCursor) {
        this.startCursor = startCursor;
    }

    public void setEndCursor(int endCursor) {
        this.endCursor = endCursor;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public UndoHistoryItemType getType() {
        return type;
    }

    public int getStartCursor() {
        return startCursor;
    }

    public int getEndCursor() {
        return endCursor;
    }

    public LinkedList<Character> getModifiedText() {
        return modifiedText;
    }

    @SuppressWarnings("unused")
    public boolean isFinished() {
        return finished;
    }

    public String getMergedModifiedText() {
        StringBuilder sb = new StringBuilder();
        for (char c : modifiedText) {
            sb.append(c);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("UndoHistoryItem{type=%s,startCursor=%d,endCursor=%d,modifiedText=\"%s\",finished=%b}",
                type,startCursor, endCursor, getMergedModifiedText(), finished);
    }
}
