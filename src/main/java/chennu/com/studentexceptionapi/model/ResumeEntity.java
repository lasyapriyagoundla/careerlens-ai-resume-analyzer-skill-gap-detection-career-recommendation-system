package chennu.com.studentexceptionapi.model;

public class ResumeEntity<T> {
    private boolean success;
    private String message;
    private T data;
    private int statusCode;

    public ResumeEntity() {}

    public ResumeEntity(boolean success, String message, T data, int statusCode) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.statusCode = statusCode;
    }

    public static <T> ResumeEntity<T> ok(T data) {
        return new ResumeEntity<>(true, "Success", data, 200);
    }

    public static <T> ResumeEntity<T> notFound() {
        return new ResumeEntity<>(false, "Not Found", null, 404);
    }

    public static <T> ResumeEntity<T> error(String message) {
        return new ResumeEntity<>(false, message, null, 500);
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }
}