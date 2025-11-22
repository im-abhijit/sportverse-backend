package co.sportverse.sportverse_backend.service;

import co.sportverse.sportverse_backend.dto.BookingItemResponse;
import co.sportverse.sportverse_backend.dto.VenueResponse;
import co.sportverse.sportverse_backend.entity.TimeSlot;
import co.sportverse.sportverse_backend.entity.Venue;
import co.sportverse.sportverse_backend.entity.VenueSlots;
import co.sportverse.sportverse_backend.entity.User;
import co.sportverse.sportverse_backend.repository.BookingRepository;
import co.sportverse.sportverse_backend.repository.PartnerRepository;
import co.sportverse.sportverse_backend.repository.SlotsRepository;
import co.sportverse.sportverse_backend.repository.VenueRepository;
import co.sportverse.sportverse_backend.service.UserService;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private SlotsRepository slotsRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private UserService userService;

    public List<BookingItemResponse> getUserBookings(String userId) {
        List<Document> bookingDocs = bookingRepository.findByUserId(userId);
        List<BookingItemResponse> responses = new ArrayList<>();

        for (Document doc : bookingDocs) {
            BookingItemResponse item = new BookingItemResponse();
            item.setId(doc.getObjectId("_id").toString());
            String venueId = doc.getObjectId("venueId").toString();
            String date = doc.getString("date");
            item.setDate(date);
            item.setAmount(doc.getInteger("amount", 0));
            item.setBookingStatus(doc.getString("bookingStatus"));

            Document paymentDoc = (Document) doc.get("payment");
            if (paymentDoc != null) {
                item.setPaymentStatus(paymentDoc.getString("status"));
            }

            Venue venue = venueRepository.findById(venueId);
            if (venue != null) {
                item.setVenue(new VenueResponse(venue));
            }

            // Check if slots are stored directly in booking (new structure)
            @SuppressWarnings("unchecked")
            List<Document> slotsDocs = (List<Document>) doc.get("slots");
            if (slotsDocs != null && !slotsDocs.isEmpty()) {
                // Use slots stored directly in booking document
                List<TimeSlot> selected = new ArrayList<>();
                for (Document slotDoc : slotsDocs) {
                    TimeSlot slot = new TimeSlot();
                    slot.setSlotId(slotDoc.getString("slotId"));
                    slot.setStartTime(slotDoc.getString("startTime"));
                    slot.setEndTime(slotDoc.getString("endTime"));
                    slot.setStartTimeAmPm(slotDoc.getString("startTimeAmPm"));
                    slot.setEndTimeAmPm(slotDoc.getString("endTimeAmPm"));
                    Object priceValue = slotDoc.get("price");
                    if (priceValue instanceof Number) {
                        slot.setPrice(((Number) priceValue).intValue());
                    }
                    slot.setBooked(slotDoc.getBoolean("isBooked", false));
                    selected.add(slot);
                }
                item.setSlots(selected);
            } else {
                // Fallback: check for slotIds (backward compatibility with old bookings)
                @SuppressWarnings("unchecked")
                List<String> slotIds = (List<String>) doc.get("slotIds");
                if (slotIds != null && !slotIds.isEmpty()) {
                    VenueSlots vs = slotsRepository.findByVenueIdAndDate(venueId, date);
                    if (vs != null && vs.getSlots() != null) {
                        Set<String> target = new HashSet<>(slotIds);
                        List<TimeSlot> selected = new ArrayList<>();
                        for (TimeSlot s : vs.getSlots()) {
                            if (s.getSlotId() != null && target.contains(s.getSlotId())) {
                                selected.add(s);
                            }
                        }
                        item.setSlots(selected);
                    } else {
                        item.setSlots(new ArrayList<>());
                    }
                } else {
                    item.setSlots(new ArrayList<>());
                }
            }

            responses.add(item);
        }

        return responses;
    }

    public List<BookingItemResponse> getPartnerBookings(String partnerId) {
        // 1. Get venueIds for this partner
        List<String> venueIds = partnerRepository.getVenueIdsByPartnerId(partnerId);
        if (venueIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. Get all bookings for these venues (already sorted newest to oldest)
        List<Document> bookingDocs = bookingRepository.findByVenueIds(venueIds);
        List<BookingItemResponse> responses = new ArrayList<>();

        // 3. Build response objects
        for (Document doc : bookingDocs) {
            BookingItemResponse item = new BookingItemResponse();
            item.setId(doc.getObjectId("_id").toString());
            String venueId = doc.getObjectId("venueId").toString();
            String date = doc.getString("date");
            item.setDate(date);
            item.setAmount(doc.getInteger("amount", 0));
            item.setBookingStatus(doc.getString("bookingStatus"));

            Document paymentDoc = (Document) doc.get("payment");
            if (paymentDoc != null) {
                item.setPaymentStatus(paymentDoc.getString("status"));
            }

            Venue venue = venueRepository.findById(venueId);
            if (venue != null) {
                item.setVenue(new VenueResponse(venue));
            }

            // Check if slots are stored directly in booking (new structure)
            @SuppressWarnings("unchecked")
            List<Document> slotsDocs = (List<Document>) doc.get("slots");
            if (slotsDocs != null && !slotsDocs.isEmpty()) {
                // Use slots stored directly in booking document
                List<TimeSlot> selected = new ArrayList<>();
                for (Document slotDoc : slotsDocs) {
                    TimeSlot slot = new TimeSlot();
                    slot.setSlotId(slotDoc.getString("slotId"));
                    slot.setStartTime(slotDoc.getString("startTime"));
                    slot.setEndTime(slotDoc.getString("endTime"));
                    slot.setStartTimeAmPm(slotDoc.getString("startTimeAmPm"));
                    slot.setEndTimeAmPm(slotDoc.getString("endTimeAmPm"));
                    Object priceValue = slotDoc.get("price");
                    if (priceValue instanceof Number) {
                        slot.setPrice(((Number) priceValue).intValue());
                    }
                    slot.setBooked(slotDoc.getBoolean("isBooked", false));
                    selected.add(slot);
                }
                item.setSlots(selected);
            } else {
                // Fallback: check for slotIds (backward compatibility with old bookings)
                @SuppressWarnings("unchecked")
                List<String> slotIds = (List<String>) doc.get("slotIds");
                if (slotIds != null && !slotIds.isEmpty()) {
                    VenueSlots vs = slotsRepository.findByVenueIdAndDate(venueId, date);
                    if (vs != null && vs.getSlots() != null) {
                        Set<String> target = new HashSet<>(slotIds);
                        List<TimeSlot> selected = new ArrayList<>();
                        for (TimeSlot s : vs.getSlots()) {
                            if (s.getSlotId() != null && target.contains(s.getSlotId())) {
                                selected.add(s);
                            }
                        }
                        item.setSlots(selected);
                    } else {
                        item.setSlots(new ArrayList<>());
                    }
                } else {
                    item.setSlots(new ArrayList<>());
                }
            }

            responses.add(item);
        }

        return responses;
    }

    public String createBooking(String partnerId, String userId, String venueId, List<co.sportverse.sportverse_backend.dto.CreateBookingRequest.SlotDto> slotDtos, String date, String status, String paymentStatus) {
        // Calculate total amount from slot DTOs and extract slot IDs
        int totalAmount = 0;
        List<String> slotIds = new ArrayList<>();
        
        if (slotDtos != null && !slotDtos.isEmpty()) {
            for (co.sportverse.sportverse_backend.dto.CreateBookingRequest.SlotDto slotDto : slotDtos) {
                if (slotDto.getPrice() > 0) {
                    totalAmount += slotDto.getPrice();
                }
                if (slotDto.getSlotId() != null) {
                    slotIds.add(slotDto.getSlotId());
                }
            }
        }
        
        // Create booking with provided status and paymentStatus, storing complete slot details
        String bookingId = bookingRepository.createBookingDirect(partnerId, userId, venueId, slotDtos, date, totalAmount, status, paymentStatus);
        
        // Only mark slots as booked if both paymentStatus and status are SUCCESS
        // For booking status, SUCCESS or CONFIRMED are considered success states
        boolean shouldMarkBooked = paymentStatus != null && "SUCCESS".equalsIgnoreCase(paymentStatus.trim()) && 
                                   status != null && ("SUCCESS".equalsIgnoreCase(status.trim()));
        
        if (shouldMarkBooked && !slotIds.isEmpty()) {
            try {
                slotsRepository.markSlotsBooked(venueId, date, slotIds);
            } catch (Exception e) {
                // Log error but don't fail booking creation
                System.err.println("Failed to mark slots as booked: " + e.getMessage());
            }
        }
        
        return bookingId;
    }
    
    public boolean confirmBooking(String bookingId) {
        if (bookingId == null || bookingId.trim().isEmpty()) {
            throw new IllegalArgumentException("Booking ID is required");
        }
        
        // Check if booking exists
        org.bson.Document booking = bookingRepository.findById(bookingId.trim());
        if (booking == null) {
            return false;
        }
        
        // Update booking status and payment status to SUCCESS
        bookingRepository.confirmBooking(bookingId.trim());
        
        // Mark slots as booked in the slots collection
        String venueId = booking.getObjectId("venueId").toString();
        String date = booking.getString("date");
        
        @SuppressWarnings("unchecked")
        List<Document> slotsDocs = (List<Document>) booking.get("slots");
        if (slotsDocs != null && !slotsDocs.isEmpty()) {
            List<String> slotIds = new ArrayList<>();
            for (Document slotDoc : slotsDocs) {
                String slotId = slotDoc.getString("slotId");
                if (slotId != null) {
                    slotIds.add(slotId);
                }
            }
            if (!slotIds.isEmpty()) {
                try {
                    slotsRepository.markSlotsBooked(venueId, date, slotIds);
                } catch (Exception e) {
                    System.err.println("Failed to mark slots as booked: " + e.getMessage());
                }
            }
        }
        
        return true;
    }

    public List<BookingItemResponse> getUserBookingsByMobileNumber(String mobileNumber) {
        // Find user by mobile number
        return getUserBookings(mobileNumber);
    }
}


