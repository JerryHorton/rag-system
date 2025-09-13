package cn.cug.sxy.ai.infrastructure.repository;

import cn.cug.sxy.ai.domain.rag.model.entity.DocumentChunk;
import cn.cug.sxy.ai.domain.rag.repository.IDocumentChunkRepository;
import cn.cug.sxy.ai.infrastructure.dao.IDocumentChunkDao;
import cn.cug.sxy.ai.infrastructure.dao.converter.DocumentChunkConverter;
import cn.cug.sxy.ai.infrastructure.dao.po.DocumentChunkPO;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @version 1.0
 * @Date 2025/9/8 17:48
 * @Description 文档块仓储实现类
 * @Author jerryhotton
 */

@Repository
public class DocumentChunkRepository implements IDocumentChunkRepository {

    private final IDocumentChunkDao documentChunkDao;
    private final DocumentChunkConverter documentChunkConverter;

    public DocumentChunkRepository(
            IDocumentChunkDao documentChunkDao,
            DocumentChunkConverter documentChunkConverter) {
        this.documentChunkDao = documentChunkDao;
        this.documentChunkConverter = documentChunkConverter;
    }

    @Override
    public DocumentChunk save(DocumentChunk chunk) {
        DocumentChunkPO po = documentChunkConverter.toDataObject(chunk);
        if (po.getId() == null) {
            documentChunkDao.insert(po);
            // 将生成的ID回填到实体对象
            chunk.setId(po.getId());
        } else {
            documentChunkDao.updateById(po);
        }

        return chunk;
    }

    @Override
    public int saveAll(List<DocumentChunk> chunks) {
        List<DocumentChunkPO> pos = documentChunkConverter.toPOList(chunks);
        int result = documentChunkDao.batchInsert(pos);
        // 回填ID到实体对象
        for (int i = 0; i < chunks.size(); i++) {
            chunks.get(i).setId(pos.get(i).getId());
        }

        return result;
    }

    @Override
    public Optional<DocumentChunk> findById(Long id) {
        DocumentChunkPO po = documentChunkDao.selectById(id);
        return Optional.ofNullable(documentChunkConverter.toEntity(po));
    }

    @Override
    public List<DocumentChunk> findByDocumentId(Long documentId) {
        List<DocumentChunkPO> pos = documentChunkDao.selectByDocumentId(documentId);
        return documentChunkConverter.toEntityList(pos);
    }

    @Override
    public List<DocumentChunk> findByConditions(Map<String, Object> conditions) {
        List<DocumentChunkPO> pos = documentChunkDao.selectByCondition(conditions);
        return documentChunkConverter.toEntityList(pos);
    }

    @Override
    public List<DocumentChunk> findAll() {
        List<DocumentChunkPO> pos = documentChunkDao.selectAll();
        return documentChunkConverter.toEntityList(pos);
    }

    @Override
    public boolean deleteById(Long id) {
        int result = documentChunkDao.deleteById(id);
        return result > 0;
    }

    @Override
    public int deleteByDocumentId(Long documentId) {
        return documentChunkDao.deleteByDocumentId(documentId);
    }

    @Override
    public boolean updateVectorized(Long id, boolean vectorized, Long vectorId) {
        int result = documentChunkDao.updateVectorized(id, vectorized, vectorId);
        return result > 0;
    }

    @Override
    public List<DocumentChunk> findNonVectorizedChunks(int limit) {
        List<DocumentChunkPO> pos = documentChunkDao.selectNonVectorizedChunks(limit);
        return documentChunkConverter.toEntityList(pos);
    }

    @Override
    public Optional<DocumentChunk> findByVectorId(String vectorId) {
        DocumentChunkPO po = documentChunkDao.selectByVectorId(vectorId);
        return Optional.ofNullable(documentChunkConverter.toEntity(po));
    }

    @Override
    public List<DocumentChunk> searchByKeyword(String keyword) {
        List<DocumentChunkPO> pos = documentChunkDao.searchByKeyword(keyword);
        return documentChunkConverter.toEntityList(pos);
    }

    @Override
    public int countNonVectorizedByDocumentId(Long documentId) {
        return documentChunkDao.countNonVectorizedByDocumentId(documentId);
    }

}
