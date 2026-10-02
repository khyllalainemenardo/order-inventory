package edu.cit.menardo.channel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "channel_cursor")
class FeedCursor {

    static final int ID = 1;

    @Id
    private int id;

    @Column(name = "last_seq")
    private long lastSeq;

    protected FeedCursor() {
    }

    FeedCursor(long lastSeq) {
        this.id = ID;
        this.lastSeq = lastSeq;
    }

    long getLastSeq() {
        return lastSeq;
    }

    void moveTo(long seq) {
        if (seq > lastSeq) {
            this.lastSeq = seq;
        }
    }
}
