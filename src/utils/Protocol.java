package utils;


public class Protocol {
    public final static String CRLF = "\r\n";
    public final static byte CR = 0xD;
    public final static byte LF = 0xA;
    public static String VERSION;
    public final static int MAX_CHUNK_SIZE = 64000;
    public final static int MAX_CHUNKS = 1000000;
    public final static int DEFAULT_DISK_SIZE = 20000000;
    public final static int CHUNK_MSG_DELAY = 400;
    public final static int STORED_MSG_DELAY = 400;
    public final static int RECLAIM_PUTCHUNK_MSG_DELAY = 400;
    public final static int PACKET_MAX_SIZE = 64000 + 1000;
    public final static int PUTCHUNK_MAX_RETRY_COUNT = 5;
    public final static int PUTCHUNK_INITIAL_WAITING_TIME = 1000;
    public final static int ENHANCED_DELETE_INITIAL_WAITING_TIME = 1000;
    public final static int DELETE_MAX_TRIES = 3;
    public final static int RESTORE_CHUNK_MAX_TRIES = 3;
    public final static int RESTORE_CHUNK_MAX_TIMEOUT_MS = 10000;
    public final static int DELAYED_ENHANCED_RECOVER_SOCKET_TIMEOUT = 5000;
	public final static int DISK_BACKUP_INTERVAL_SECONDS = 5;
	public final static int MC_THREAD_POOL_SIZE = 10;
	public static final int TASK_MANAGER_THREAD_POLL_SIZE = 20;
}