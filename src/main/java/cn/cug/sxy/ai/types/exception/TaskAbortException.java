package cn.cug.sxy.ai.types.exception;

/**
 * 当任务编排需要终止时抛出的异常。
 */
public class TaskAbortException extends RuntimeException {
    public TaskAbortException(String message) {
        super(message);
    }

    public TaskAbortException(String message, Throwable cause) {
        super(message, cause);
    }
}

