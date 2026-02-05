package com.tradeoption.service;

import com.tradeoption.domain.Position;
import com.tradeoption.domain.PositionEntry;
import com.tradeoption.domain.PositionPnl;
import com.tradeoption.domain.TradeAction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class PositionPnlCalculator {

    public static PositionPnl calculatePnl(Position position, double currentLtp) {
        List<PositionEntry> entries = new ArrayList<>(position.getEntries());
        // Sort by timestamp to ensure FIFO
        entries.sort(Comparator.comparingLong(PositionEntry::getTimestamp));

        List<MatchableEntry> buys = entries.stream()
                .filter(e -> e.getAction() == TradeAction.BUY)
                .map(MatchableEntry::new)
                .collect(Collectors.toList());

        List<MatchableEntry> sells = entries.stream()
                .filter(e -> e.getAction() == TradeAction.SELL)
                .map(MatchableEntry::new)
                .collect(Collectors.toList());

        double realizedPnl = 0.0;

        // 1. Process Linked Entries First
        // Logic: Iterate through sells (if closing buys) or buys (if closing sells).
        // Simpler approach: Iterate all entries, if it has link, try to find and match.
        // But entries are separated.

        // We need to know which side is "opening" and which is "closing".
        // In options, it's net quantity based. But for partial match, we match opposing
        // sides.

        // Let's iterate through Sells to match against Buys (and vice versa for short
        // selling?)
        // Standard FIFO PNL: Match Sells against Buys.
        // If Short Selling is allowed, we might have net negative.

        // To handle both Long and Short lifecycles uniformly:
        // We match "smaller side" against "larger side"? No, that's not right. FIFO is
        // temporal.

        // Correct FIFO/Linked Matcher:
        // Identify "Open" bucket and "Close" bucket?
        // Actually, we just match Buy vs Sell until one runs out.

        // Linked Matching Round
        realizedPnl += matchLinked(buys, sells);

        // FIFO Matching Round (for remaining)
        realizedPnl += matchFifo(buys, sells);

        // Remaining unmatched entries form the "Open Position"
        int netQty = 0;
        double totalOpenCost = 0;
        int totalOpenQty = 0;

        // Calculate Unrealized from remaining Buys
        for (MatchableEntry b : buys) {
            if (b.remainingQty > 0) {
                netQty += b.remainingQty;
                totalOpenCost += (b.entry.getPrice() * b.remainingQty);
                totalOpenQty += b.remainingQty;
            }
        }

        // Calculate Unrealized from remaining Sells (Short position)
        for (MatchableEntry s : sells) {
            if (s.remainingQty > 0) {
                netQty -= s.remainingQty; // Short quantity
                totalOpenCost -= (s.entry.getPrice() * s.remainingQty); // Cost is negative for short?
                // For avg price calc:
                // Avg Price = Total Value / Total Qty.
                // For shorts, we tracked collected premium.
                totalOpenQty += s.remainingQty;
            }
        }

        // Avg Price of Open Position
        double avgPrice = totalOpenQty == 0 ? 0 : Math.abs(totalOpenCost / totalOpenQty);

        // Unrealized PNL
        // If Net Long: (LTP - Avg) * Qty
        // If Net Short: (Avg - LTP) * Qty
        double unrealizedPnl = 0;
        if (netQty > 0) {
            unrealizedPnl = (currentLtp - avgPrice) * netQty;
        } else if (netQty < 0) {
            unrealizedPnl = (avgPrice - currentLtp) * Math.abs(netQty);
        }

        return new PositionPnl(realizedPnl, unrealizedPnl, netQty, avgPrice);
    }

    private static double matchLinked(List<MatchableEntry> buys, List<MatchableEntry> sells) {
        double pnl = 0;
        // Check Sells linking to Buys
        for (MatchableEntry sell : sells) {
            if (sell.remainingQty > 0 && sell.entry.getLinkedEntryId() != null) {
                // Find target Buy
                for (MatchableEntry buy : buys) {
                    if (buy.entry.getId().equals(sell.entry.getLinkedEntryId()) && buy.remainingQty > 0) {
                        pnl += executeMatch(buy, sell);
                    }
                }
            }
        }

        // Check Buys linking to Sells (Short Cover) - uncommon but possible if
        // re-entering?
        // For simplicity, usually Linked Exit is Action closing an Open.
        // Assuming user links "Exit" order to "Entry" order ID.
        return pnl;
    }

    private static double matchFifo(List<MatchableEntry> buys, List<MatchableEntry> sells) {
        double pnl = 0;
        int buyIdx = 0;
        int sellIdx = 0;

        while (buyIdx < buys.size() && sellIdx < sells.size()) {
            MatchableEntry buy = buys.get(buyIdx);
            MatchableEntry sell = sells.get(sellIdx);

            // Skip if exhausted (e.g. by linked match)
            if (buy.remainingQty == 0) {
                buyIdx++;
                continue;
            }
            if (sell.remainingQty == 0) {
                sellIdx++;
                continue;
            }

            // Execute Match
            pnl += executeMatch(buy, sell);
        }
        return pnl;
    }

    private static double executeMatch(MatchableEntry buy, MatchableEntry sell) {
        int matchedQty = Math.min(buy.remainingQty, sell.remainingQty);

        buy.remainingQty -= matchedQty;
        sell.remainingQty -= matchedQty;

        // PNL = (Sell Price - Buy Price) * Qty
        return (sell.entry.getPrice() - buy.entry.getPrice()) * matchedQty;
    }

    private static class MatchableEntry {
        PositionEntry entry;
        int remainingQty;

        MatchableEntry(PositionEntry entry) {
            this.entry = entry;
            this.remainingQty = entry.getQuantity();
        }
    }
}
