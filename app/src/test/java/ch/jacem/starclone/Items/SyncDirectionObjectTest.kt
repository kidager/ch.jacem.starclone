package ch.jacem.starclone.Items

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncDirectionObjectTest {

    @Test
    fun isRemoteToRemote_syncR2R_returnsTrue() {
        assertTrue(SyncDirectionObject.isRemoteToRemote(SyncDirectionObject.SYNC_REMOTE_TO_REMOTE))
    }

    @Test
    fun isRemoteToRemote_copyR2R_returnsTrue() {
        assertTrue(SyncDirectionObject.isRemoteToRemote(SyncDirectionObject.COPY_REMOTE_TO_REMOTE))
    }

    @Test
    fun isRemoteToRemote_syncLocalToRemote_returnsFalse() {
        assertFalse(SyncDirectionObject.isRemoteToRemote(SyncDirectionObject.SYNC_LOCAL_TO_REMOTE))
    }

    @Test
    fun isRemoteToRemote_syncRemoteToLocal_returnsFalse() {
        assertFalse(SyncDirectionObject.isRemoteToRemote(SyncDirectionObject.SYNC_REMOTE_TO_LOCAL))
    }

    @Test
    fun isRemoteToRemote_copyLocalToRemote_returnsFalse() {
        assertFalse(SyncDirectionObject.isRemoteToRemote(SyncDirectionObject.COPY_LOCAL_TO_REMOTE))
    }

    @Test
    fun isRemoteToRemote_copyRemoteToLocal_returnsFalse() {
        assertFalse(SyncDirectionObject.isRemoteToRemote(SyncDirectionObject.COPY_REMOTE_TO_LOCAL))
    }

    @Test
    fun isRemoteToRemote_syncBidirectional_returnsFalse() {
        assertFalse(SyncDirectionObject.isRemoteToRemote(SyncDirectionObject.SYNC_BIDIRECTIONAL))
    }

    @Test
    fun isRemoteToRemote_syncBidirectionalInitial_returnsFalse() {
        assertFalse(SyncDirectionObject.isRemoteToRemote(SyncDirectionObject.SYNC_BIDIRECTIONAL_INITIAL))
    }

    @Test
    fun constants_haveExpectedValues() {
        // Verify constants haven't been accidentally changed
        assertTrue(SyncDirectionObject.SYNC_LOCAL_TO_REMOTE == 1)
        assertTrue(SyncDirectionObject.SYNC_REMOTE_TO_LOCAL == 2)
        assertTrue(SyncDirectionObject.COPY_LOCAL_TO_REMOTE == 3)
        assertTrue(SyncDirectionObject.COPY_REMOTE_TO_LOCAL == 4)
        assertTrue(SyncDirectionObject.SYNC_BIDIRECTIONAL_INITIAL == 5)
        assertTrue(SyncDirectionObject.SYNC_BIDIRECTIONAL == 6)
        assertTrue(SyncDirectionObject.SYNC_REMOTE_TO_REMOTE == 7)
        assertTrue(SyncDirectionObject.COPY_REMOTE_TO_REMOTE == 8)
    }
}
