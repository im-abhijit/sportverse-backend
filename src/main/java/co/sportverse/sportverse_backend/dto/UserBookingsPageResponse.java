package co.sportverse.sportverse_backend.dto;

import java.util.List;

public class UserBookingsPageResponse {

    private List<BookingItemResponse> items;
    private int page;
    private int pageSize;

    public UserBookingsPageResponse() {}

    public UserBookingsPageResponse(List<BookingItemResponse> items, int page, int pageSize) {
        this.items = items;
        this.page = page;
        this.pageSize = pageSize;
    }

    public List<BookingItemResponse> getItems() {
        return items;
    }

    public void setItems(List<BookingItemResponse> items) {
        this.items = items;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
