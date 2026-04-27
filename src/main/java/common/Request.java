package common;

import java.io.Serializable;

public class Request implements Serializable {
    // Đảm bảo tính nhất quán của object khi gửi qua mạng giữa các phiên bản java
    private static final long serialVersionUID = 1L;

    // Action là để biết phía Client đang cần gì
    private String action;

    // Nội dung gói tin
    private Object data;

    public Request(String action, Object data) {
        this.action = action;
        this.data = data;
    }

    public String getAction() {
        return action;
    }

    public Object getData() {
        return data;
    }
}
