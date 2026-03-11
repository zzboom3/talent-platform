package com.talent.platform.service;

import com.talent.platform.entity.BlockchainBlock;
import com.talent.platform.repository.BlockchainBlockRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlockchainService {

    private static final String GENESIS_DATA = "{\"type\":\"genesis\",\"message\":\"Talent Platform Blockchain\"}";

    private final BlockchainBlockRepository blockRepository;

    @PostConstruct
    public void init() {
        if (blockRepository.count() == 0) {
            BlockchainBlock genesis = new BlockchainBlock();
            genesis.setBlockIndex(0);
            genesis.setPreviousHash("0");
            genesis.setTimestamp(System.currentTimeMillis());
            genesis.setData(GENESIS_DATA);
            genesis.setNonce(0);
            genesis.setHash(calculateHash(0, "0", genesis.getTimestamp(), GENESIS_DATA, 0));
            blockRepository.save(genesis);
            log.info("Genesis block created");
        }
    }

    public BlockchainBlock addBlock(String data) {
        BlockchainBlock lastBlock = blockRepository.findTopByOrderByBlockIndexDesc()
                .orElseThrow(() -> new IllegalStateException("Blockchain not initialized"));
        int newIndex = lastBlock.getBlockIndex() + 1;
        long timestamp = System.currentTimeMillis();
        int nonce = 0;
        String previousHash = lastBlock.getHash();
        String hash = calculateHash(newIndex, previousHash, timestamp, data, nonce);

        BlockchainBlock newBlock = new BlockchainBlock();
        newBlock.setBlockIndex(newIndex);
        newBlock.setPreviousHash(previousHash);
        newBlock.setHash(hash);
        newBlock.setData(data);
        newBlock.setTimestamp(timestamp);
        newBlock.setNonce(nonce);
        return blockRepository.save(newBlock);
    }

    public boolean verifyChain() {
        List<BlockchainBlock> blocks = blockRepository.findAllByOrderByBlockIndexAsc();
        if (blocks.isEmpty()) {
            return false;
        }
        for (int i = 0; i < blocks.size(); i++) {
            BlockchainBlock block = blocks.get(i);
            String expectedHash = calculateHash(
                    block.getBlockIndex(),
                    block.getPreviousHash(),
                    block.getTimestamp(),
                    block.getData(),
                    block.getNonce());
            if (!expectedHash.equals(block.getHash())) {
                return false;
            }
            if (i > 0 && !block.getPreviousHash().equals(blocks.get(i - 1).getHash())) {
                return false;
            }
        }
        return true;
    }

    public String calculateHash(int index, String previousHash, long timestamp, String data, int nonce) {
        String input = index + previousHash + timestamp + data + nonce;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
