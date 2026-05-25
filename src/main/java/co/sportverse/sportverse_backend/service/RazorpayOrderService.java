package co.sportverse.sportverse_backend.service;

import co.sportverse.sportverse_backend.config.RazorpayConfig;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RazorpayOrderService {

    @Autowired
    private RazorpayConfig razorpayConfig;

    private RazorpayClient client(RazorpayConfig.ResolvedKeys keys) throws RazorpayException {
        return new RazorpayClient(keys.keyId(), keys.keySecret());
    }

    public RazorpayOrderResult createOrder(int amountInRupees, String receipt, String payerPhoneForCredentials) {
        if (amountInRupees <= 0) {
            throw new IllegalArgumentException("Amount must be > 0");
        }
        if (receipt == null || receipt.trim().isEmpty()) {
            throw new IllegalArgumentException("receipt is required");
        }

        RazorpayConfig.ResolvedKeys keys = razorpayConfig.resolveKeysForPayerPhone(payerPhoneForCredentials);

        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInRupees * 100);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", receipt.trim().length() <= 40 ? receipt.trim() : receipt.trim().substring(0, 40));

            Order order = client(keys).orders.create(orderRequest);
            return new RazorpayOrderResult(
                    order.get("id"),
                    order.get("amount"),
                    order.get("currency"),
                    keys.keyId()
            );
        } catch (RazorpayException e) {
            throw new RuntimeException("Failed to create Razorpay order: " + e.getMessage(), e);
        }
    }

    public static class RazorpayOrderResult {
        private final String orderId;
        private final int amount;
        private final String currency;
        private final String key;

        public RazorpayOrderResult(String orderId, int amount, String currency, String key) {
            this.orderId = orderId;
            this.amount = amount;
            this.currency = currency;
            this.key = key;
        }

        public String getOrderId() {
            return orderId;
        }

        public int getAmount() {
            return amount;
        }

        public String getCurrency() {
            return currency;
        }

        public String getKey() {
            return key;
        }
    }
}
