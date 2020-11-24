package no.ssb.forbruk.nets.storage.utils;

import de.huxhorn.sulky.ulid.ULID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class ULIDGenerator {
    private static final Logger logger = LoggerFactory.getLogger(ULIDGenerator.class);

    static final AtomicReference<ULID> ulid = new AtomicReference<>(new ULID());
    static final AtomicReference<ULID.Value> prevUlid = new AtomicReference<>(ulid.get().nextValue());

    static ULID.Value nextMonotonicUlid() {
        /*
         * Will spin until time ticks if next value overflows.
         * Although theoretically possible, it is extremely unlikely that the loop will ever spin
         */
        ULID.Value value;
        ULID.Value previousUlid = prevUlid.get();
        do {
            long timestamp = System.currentTimeMillis();
            long diff = timestamp - previousUlid.timestamp();
            if (diff < 0) {
                if (diff < -(30 * 1000)) {
                    throw new IllegalStateException(String.format("Previous timestamp is in the future. Diff %d ms", -diff));
                }
                logger.debug("Previous timestamp is in the future, waiting for time to catch up. Diff {} ms", -diff);
                try {
                    Thread.sleep(-diff);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } else if (diff > 0) {
                // start at lsb 1, to avoid inclusive/exclusive semantics when searching
                value = new ULID.Value((timestamp << 16) & 0xFFFFFFFFFFFF0000L, 1L);
                prevUlid.set(value);
                return value;
            }
            // diff == 0
            value = ulid.get().nextStrictlyMonotonicValue(previousUlid, timestamp).orElse(null);
            prevUlid.set(value);
        } while (value == null);
        prevUlid.set(value);
        return value;
    }

    public static ULID.Value generate() {
        return nextMonotonicUlid();
    }

    public static UUID toUUID(ULID.Value ulid) {
        return new UUID(ulid.getMostSignificantBits(), ulid.getLeastSignificantBits());
    }
}
