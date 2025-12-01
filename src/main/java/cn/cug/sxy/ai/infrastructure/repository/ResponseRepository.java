package cn.cug.sxy.ai.infrastructure.repository;

import cn.cug.sxy.ai.domain.rag.model.entity.Response;
import cn.cug.sxy.ai.domain.rag.repository.IResponseRepository;
import cn.cug.sxy.ai.infrastructure.dao.postgres.IResponseDao;
import cn.cug.sxy.ai.infrastructure.dao.converter.ResponseConverter;
import cn.cug.sxy.ai.infrastructure.dao.po.ResponsePO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @version 1.0
 * @Date 2025/9/11 11:19
 * @Description 响应仓储实现类
 * @Author jerryhotton
 */

@Slf4j
@Repository
public class ResponseRepository implements IResponseRepository {

    private final IResponseDao responseDao;
    private final ResponseConverter responseConverter;

    public ResponseRepository(
            IResponseDao responseDao,
            ResponseConverter responseConverter) {
        this.responseDao = responseDao;
        this.responseConverter = responseConverter;
    }

    @Override
    public Response save(Response response) {
        ResponsePO po = responseConverter.toPO(response);
        if (po.getId() == null) {
            responseDao.insert(po);
            // 将生成的ID回填到实体对象
            response.setId(po.getId());
        } else {
            responseDao.updateById(po);
        }

        return response;
    }

    @Override
    public Optional<Response> findById(Long id) {
        ResponsePO po = responseDao.selectById(id);
        return Optional.ofNullable(responseConverter.toEntity(po));
    }

    @Override
    public List<Response> findByQueryId(Long queryId) {
        List<ResponsePO> pos = responseDao.selectByQueryId(queryId);
        return responseConverter.toEntities(pos);
    }

    @Override
    public List<Response> findBySessionId(String sessionId, int limit) {
        List<ResponsePO> pos = responseDao.selectBySessionId(sessionId, limit);
        return responseConverter.toEntities(pos);
    }

    @Override
    public boolean deleteById(Long id) {
        int result = responseDao.deleteById(id);
        return result > 0;
    }

    @Override
    public int countByQueryId(Long queryId) {
        return responseDao.countByQueryId(queryId);
    }

    @Override
    public List<Map<String, Object>> findSourceChunksByDocumentId(Long documentId) {
        return responseDao.selectSourceChunksByDocumentId(documentId);
    }

}
