package co.sportverse.sportverse_backend.service;

import co.sportverse.sportverse_backend.config.RazorpayConfig;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Refund;
import org.bson.Document;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@Service
public class RazorpayRefundService {

    @Autowired
    private RazorpayConfig razorpayConfig;

    private RazorpayClient client(RazorpayConfig.ResolvedKeys keys) throws RazorpayException {
        return new RazorpayClient(keys.keyId(), keys.keySecret());
    }

    /**
     * Full refund for a captured payment (amount omitted → entire capture).
     *
     * @return Mongo subdocument fields for {@code refundDto}; includes {@code Razorpay}-sourced {@code status}.
     */
    public Document createFullRefundDocument(String razorpayPaymentId, String receipt, String bookingOwnerPhoneRaw) {
        if (razorpayPaymentId == null || razorpayPaymentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Razorpay payment id is required");
        }
        RazorpayConfig.ResolvedKeys keys = razorpayConfig.resolveKeysForPayerPhone(bookingOwnerPhoneRaw);
        try {
            JSONObject body = new JSONObject();
            body.put("speed", "normal");
            if (receipt != null && !receipt.isBlank()) {
                String rec = receipt.trim();
                if (rec.length() > 255) {
                    rec = rec.substring(0, 255);
                }
                body.put("receipt", rec);
            }

            Refund refund = client(keys).payments.refund(razorpayPaymentId.trim(), body);
            JSONObject json = refund.toJson();
            Document doc = new Document();

            doc.append("status", json.has("status") ? json.getString("status") : null);
            doc.append("refundId", json.has("id") ? json.getString("id") : null);
            if (json.has("amount")) {
                Object a = json.get("amount");
                if (a instanceof Number n) {
                    doc.append("amount", n.longValue());
                } else {
                    doc.append("amount", Long.parseLong(String.valueOf(a)));
                }
            }
            if (json.has("payment_id")) {
                doc.append("paymentId", json.getString("payment_id"));
            }
            if (json.has("created_at")) {
                Object c = json.get("created_at");
                if (c instanceof Number n) {
                    doc.append("createdAt", n.longValue());
                } else {
                    doc.append("createdAt", Long.parseLong(String.valueOf(c)));
                }
            }

            List<String> acqPairs = new ArrayList<>();
            JSONObject acquirer = json.optJSONObject("acquirer_data");
            if (acquirer != null) {
                Iterator<String> itr = acquirer.keys();
                while (itr.hasNext()) {
                    String key = itr.next();
                    Object value = acquirer.get(key);
                    acqPairs.add(key + "=" + (value != null ? value.toString() : ""));
                }
                Collections.sort(acqPairs);
            }
            doc.append("acquirer_data", acqPairs);

            if (json.has("speed_requested")) {
                doc.append("speed", json.getString("speed_requested"));
            }

            return doc;
        } catch (RazorpayException e) {
            throw new IllegalStateException("Razorpay refund failed: " + e.getMessage(), e);
        }
    }
}
