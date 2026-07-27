package org.zmreborn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DockDragTransactionTest {
    @Test
    public void movingPastSourceAccountsForSourceBeforeInsertion() {
        DockDragTransaction transaction = new DockDragTransaction(1);

        transaction.stageDrop(4);

        assertEquals(3, transaction.getInsertionIndex(4));
    }

    @Test
    public void movingBeforeSourceKeepsDropIndex() {
        DockDragTransaction transaction = new DockDragTransaction(2);

        transaction.stageDrop(0);

        assertEquals(0, transaction.getInsertionIndex(4));
    }

    @Test
    public void insertionIndexClampsToRemainingChildren() {
        DockDragTransaction transaction = new DockDragTransaction(0);

        transaction.stageDrop(9);

        assertEquals(0, transaction.getInsertionIndex(1));
    }

    @Test
    public void canceledTransactionIgnoresLaterDropUpdates() {
        DockDragTransaction transaction = new DockDragTransaction(1);

        assertTrue(transaction.cancel());
        transaction.stageDrop(0);

        assertEquals(1, transaction.getInsertionIndex(4));
        assertFalse(transaction.isSuccessful());
    }

    @Test
    public void packageRemovalCancelsActiveDragBeforeLaterCompletion() {
        DragCancellationState state = new DragCancellationState();
        DockDragTransaction transaction = new DockDragTransaction(1);
        state.setDropTargetActive(true);

        assertTrue(state.consumeDropTargetExit());
        assertTrue(transaction.cancel());
        assertFalse(transaction.finish(true));
        assertFalse(transaction.isSuccessful());
    }

    @Test
    public void unrelatedPackageRemovalCancelsBeforeSourceIndexChanges() {
        DockDragTransaction transaction = new DockDragTransaction(1);

        assertTrue(transaction.cancel());
        transaction.stageDrop(2);

        assertFalse(transaction.finish(true));
        assertEquals(1, transaction.getInsertionIndex(2));
    }

    @Test
    public void otherTargetRejectionReportsMoveFailure() {
        assertTrue(DockDragTransaction.shouldShowCouldNotMove(false, true));
    }

    @Test
    public void dockRejectionThenExitReportsCancellation() {
        assertFalse(DockDragTransaction.shouldShowCouldNotMove(false, false));
    }

    @Test
    public void noTargetReleaseReportsCancellation() {
        assertFalse(DockDragTransaction.shouldShowCouldNotMove(false, false));
    }

    @Test
    public void cancelDispatchesDropTargetExitOnlyOnce() {
        DragCancellationState state = new DragCancellationState();
        state.setDropTargetActive(true);

        assertTrue(state.consumeDropTargetExit());
        assertFalse(state.consumeDropTargetExit());
    }

    @Test
    public void completionIsIdempotent() {
        DockDragTransaction transaction = new DockDragTransaction(0);

        assertTrue(transaction.finish(true));
        assertFalse(transaction.finish(false));
        assertTrue(transaction.isSuccessful());
    }

    @Test
    public void dragFromFirstToLastPosition() {
        DockDragTransaction transaction = new DockDragTransaction(0);

        transaction.stageDrop(9);

        assertEquals(8, transaction.getInsertionIndex(9));
        assertTrue(transaction.finish(true));
        assertTrue(transaction.isSuccessful());
    }

    @Test
    public void dragFromLastToFirstPosition() {
        DockDragTransaction transaction = new DockDragTransaction(9);

        transaction.stageDrop(0);

        assertEquals(0, transaction.getInsertionIndex(10));
        assertTrue(transaction.finish(true));
        assertTrue(transaction.isSuccessful());
    }

    @Test
    public void dragToSamePositionRemains() {
        DockDragTransaction transaction = new DockDragTransaction(5);

        transaction.stageDrop(5);

        assertEquals(5, transaction.getInsertionIndex(10));
    }

    @Test
    public void multipleDropUpdatesUseLast() {
        DockDragTransaction transaction = new DockDragTransaction(2);

        transaction.stageDrop(5);
        transaction.stageDrop(3);
        transaction.stageDrop(7);

        assertEquals(6, transaction.getInsertionIndex(8));
    }

    @Test
    public void zeroSizedContainerHandled() {
        DockDragTransaction transaction = new DockDragTransaction(0);

        transaction.stageDrop(0);

        assertEquals(0, transaction.getInsertionIndex(0));
    }

    @Test
    public void cancelBeforeFinishPreventsSuccess() {
        DockDragTransaction transaction = new DockDragTransaction(2);

        assertTrue(transaction.cancel());
        assertFalse(transaction.finish(true));
        assertFalse(transaction.isSuccessful());
    }
}
