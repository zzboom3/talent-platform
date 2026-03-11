package com.talent.platform.repository;

import com.talent.platform.entity.BlockchainBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BlockchainBlockRepository extends JpaRepository<BlockchainBlock, Long> {
    Optional<BlockchainBlock> findTopByOrderByBlockIndexDesc();
    Optional<BlockchainBlock> findByHash(String hash);
    List<BlockchainBlock> findAllByOrderByBlockIndexAsc();
    List<BlockchainBlock> findAllByOrderByBlockIndexDesc();
}
