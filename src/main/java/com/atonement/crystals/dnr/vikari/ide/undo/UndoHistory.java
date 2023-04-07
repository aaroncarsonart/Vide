package com.atonement.crystals.dnr.vikari.ide.undo;

import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import java.util.ArrayList;
import java.util.List;

public class UndoHistory {
    private List<UndoHistoryItem> historyItems;
    private int position = -1;
    StyledDocument document;
    private boolean enabled;

    public UndoHistory(StyledDocument document) {
        this.document = document;
        this.historyItems = new ArrayList<>();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean canUndo() {
        return !historyItems.isEmpty() && position >= 0;
    }

    public void undo() {
        enabled = false;
        UndoHistoryItem historyItem = historyItems.get(position--);
        historyItem.setFinished(true);
        if (historyItem.getType() == UndoHistoryItemType.INSERT_TEXT) {
            // remove inserted text.
            int startCursor = historyItem.getStartCursor();
            int length = historyItem.getEndCursor() - startCursor;

            try {
                document.remove(startCursor, length);
            } catch (BadLocationException e) {
                throw new RuntimeException(e);
            }
        } else if (historyItem.getType() == UndoHistoryItemType.REMOVE_TEXT) {
            // insert removed text.
            int startCursor = historyItem.getStartCursor();
            String removedText = historyItem.getMergedModifiedText();

            try {
                document.insertString(startCursor, removedText, null);
            } catch (BadLocationException e) {
                throw new RuntimeException(e);
            }
        }
        enabled = true;
    }

    public boolean canRedo() {
        return !historyItems.isEmpty() && position < historyItems.size() - 1;
    }

    public void redo() {
        enabled = false;
        UndoHistoryItem historyItem = historyItems.get(++position);
        historyItem.setFinished(true);
        if (historyItem.getType() == UndoHistoryItemType.INSERT_TEXT) {
            // insert inserted text.
            int startCursor = historyItem.getStartCursor();
            String removedText = historyItem.getMergedModifiedText();

            try {
                document.insertString(startCursor, removedText, null);
            } catch (BadLocationException e) {
                throw new RuntimeException(e);
            }

        } else if (historyItem.getType() == UndoHistoryItemType.REMOVE_TEXT) {
            // remove removed text.
            int startCursor = historyItem.getStartCursor();
            int length = historyItem.getEndCursor() - startCursor;

            try {
                document.remove(startCursor, length);
            } catch (BadLocationException e) {
                throw new RuntimeException(e);
            }
        }
        enabled = true;
    }

    public void addHistoryItem(UndoHistoryItem item) {
        if (!enabled) {
            return;
        }
        if (position == -1) {
            historyItems.add(++position, item);
            System.out.println("Added: " + item);
        } else {
//            UndoHistoryItem last = historyItems.get(position);
//            if (!last.isFinished() && mergable(last, item)) {
//                merge(last, item);
//            } else {
                historyItems.add(++position, item);
                System.out.println("Added: " + item);
//            }

            // remove dead entries
            for (int i = position + 1; i < historyItems.size(); i++) {
                historyItems.remove(i);
            }
        }
    }

    private boolean mergable(UndoHistoryItem existingItem, UndoHistoryItem newItem) {
        if (existingItem.getType() == UndoHistoryItemType.INSERT_TEXT &&
                newItem.getType() == UndoHistoryItemType.INSERT_TEXT) {
            // end of existing cursor must be adjacent to start of new cursor
            int existingEndCursor = existingItem.getEndCursor();
            int newStartCursor = newItem.getStartCursor();
            return existingEndCursor + 1 == newStartCursor;
        } else if (existingItem.getType() == UndoHistoryItemType.REMOVE_TEXT &&
                newItem.getType() == UndoHistoryItemType.REMOVE_TEXT) {
            // start of existing cursor must be adjacent to end of new cursor
            int existingStartCursor = existingItem.getStartCursor();
            int newEndCursor = newItem.getEndCursor();
            return existingStartCursor - 1 == newEndCursor;
        }
        return false;
    }

    private void merge(UndoHistoryItem existingItem, UndoHistoryItem newItem) {
        if (existingItem.getType() == UndoHistoryItemType.INSERT_TEXT &&
                newItem.getType() == UndoHistoryItemType.INSERT_TEXT) {

            List<Character> modifiedText = existingItem.getModifiedText();
            for (int i = 0; i < newItem.getModifiedText().size(); i++) {
                Character c = newItem.getModifiedText().get(i);
                modifiedText.add(c);
            }
            existingItem.setEndCursor(newItem.getEndCursor());

        } else if (existingItem.getType() == UndoHistoryItemType.REMOVE_TEXT &&
                newItem.getType() == UndoHistoryItemType.REMOVE_TEXT) {

            List<Character> modifiedText = existingItem.getModifiedText();
            for (int i = newItem.getModifiedText().size() - 1; i >= 0; i--) {
                Character c = newItem.getModifiedText().get(i);
                modifiedText.add(0, c);
            }
            existingItem.setStartCursor(newItem.getStartCursor());

        } else {
            throw new IllegalStateException("Merged UndoHistoryItem types should be the same," +
                    "but are different: existingItem: " + existingItem + ", newItem: " + newItem);
        }
    }

    public void clear() {
        historyItems.clear();
        position = -1;
    }
}
