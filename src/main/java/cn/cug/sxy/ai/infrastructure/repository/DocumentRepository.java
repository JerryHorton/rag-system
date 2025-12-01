package cn.cug.sxy.ai.infrastructure.repository;

import cn.cug.sxy.ai.domain.rag.repository.IDocumentRepository;
import cn.cug.sxy.ai.infrastructure.dao.postgres.IDocumentDao;
import cn.cug.sxy.ai.infrastructure.dao.converter.DocumentConverter;
import cn.cug.sxy.ai.infrastructure.dao.po.DocumentPO;
import cn.cug.sxy.ai.domain.rag.model.entity.Document;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @version 1.0
 * @Date 2025/9/9 09:11
 * @Description 文档仓储实现
 * @Author jerryhotton
 */

@Repository
public class DocumentRepository implements IDocumentRepository {

    private final IDocumentDao documentDao;
    private final DocumentConverter documentConverter;

    public DocumentRepository(
            IDocumentDao documentDao,
            DocumentConverter documentConverter) {
        this.documentDao = documentDao;
        this.documentConverter = documentConverter;
    }

    @Override
    public Document save(Document document) {
        DocumentPO po = documentConverter.toPO(document);
        if (po.getId() == null) {
            documentDao.insert(po);
            // 将生成的ID回填到实体对象
            document.setId(po.getId());
        } else {
            documentDao.updateById(po);
        }

        return document;
    }

    @Override
    public int saveAll(List<Document> documents) {
        List<DocumentPO> pos = documentConverter.toPOList(documents);
        int result = documentDao.batchInsert(pos);
        // 回填ID到实体对象
        for (int i = 0; i < documents.size(); i++) {
            documents.get(i).setId(pos.get(i).getId());
        }

        return result;
    }

    @Override
    public Optional<Document> findById(Long id) {
        DocumentPO po = documentDao.selectById(id);
        return Optional.ofNullable(documentConverter.toEntity(po));
    }

    @Override
    public List<Document> findByStatus(String status) {
        List<DocumentPO> pos = documentDao.selectByStatus(status);
        return documentConverter.toEntityList(pos);
    }

    @Override
    public List<Document> findByConditions(Map<String, Object> conditions) {
        List<DocumentPO> pos = documentDao.selectByCondition(conditions);
        return documentConverter.toEntityList(pos);
    }

    @Override
    public List<Document> findAll() {
        List<DocumentPO> pos = documentDao.selectAll();
        return documentConverter.toEntityList(pos);
    }

    @Override
    public Map<String, Object> findWithPagination(int page, int size, String status) {
        Page<DocumentPO> pageResult = PageHelper.startPage(page + 1, size);
        List<DocumentPO> pos;
        if (status != null && !status.isEmpty()) {
            pos = documentDao.selectByStatus(status);
        } else {
            pos = documentDao.selectAll();
        }
        List<Document> documents = documentConverter.toEntityList(pos);
        Map<String, Object> result = new HashMap<>();
        result.put("documents", documents);
        result.put("total", pageResult.getTotal());
        result.put("pages", pageResult.getPages());
        result.put("page", page);
        result.put("size", size);

        return result;
    }

    @Override
    public boolean deleteById(Long id) {
        int result = documentDao.deleteById(id);
        return result > 0;
    }

    @Override
    public boolean updateStatus(Long id, String status, String errorMessage) {
        int result = documentDao.updateStatus(id, status, errorMessage);
        return result > 0;
    }

    @Override
    public boolean updateVectorized(Long id, boolean vectorized) {
        int result = documentDao.updateVectorized(id, vectorized);
        return result > 0;
    }

    @Override
    public List<Document> findNonVectorizedDocuments(int limit) {
        List<DocumentPO> pos = documentDao.selectNonVectorizedDocuments(limit);
        return documentConverter.toEntityList(pos);
    }

    @Override
    public List<Document> searchByKeyword(String keyword) {
        List<DocumentPO> pos = documentDao.searchByKeyword(keyword);
        return documentConverter.toEntityList(pos);
    }

}
