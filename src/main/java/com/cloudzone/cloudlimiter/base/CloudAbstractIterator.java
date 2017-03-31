package com.cloudzone.cloudlimiter.base;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author tantexian
 * @since 2017/3/31
 */
@CloudGwtCompatible
abstract class CloudAbstractIterator<T> implements Iterator<T> {
    private CloudAbstractIterator.State state;
    private T next;

    protected CloudAbstractIterator() {
        this.state = CloudAbstractIterator.State.NOT_READY;
    }

    protected abstract T computeNext();

    protected final T endOfData() {
        this.state = CloudAbstractIterator.State.DONE;
        return null;
    }

    public final boolean hasNext() {
        CloudPreconditions.checkState(this.state != CloudAbstractIterator.State.FAILED);
        switch (this.state.ordinal()) {
            case 1:
                return false;
            case 2:
                return true;
            default:
                return this.tryToComputeNext();
        }
    }

    private boolean tryToComputeNext() {
        this.state = CloudAbstractIterator.State.FAILED;
        this.next = this.computeNext();
        if (this.state != CloudAbstractIterator.State.DONE) {
            this.state = CloudAbstractIterator.State.READY;
            return true;
        } else {
            return false;
        }
    }

    public final T next() {
        if (!this.hasNext()) {
            throw new NoSuchElementException();
        } else {
            this.state = CloudAbstractIterator.State.NOT_READY;
            T result = this.next;
            this.next = null;
            return result;
        }
    }

    public final void remove() {
        throw new UnsupportedOperationException();
    }

    private static enum State {
        READY,
        NOT_READY,
        DONE,
        FAILED;

        private State() {
        }
    }
}
