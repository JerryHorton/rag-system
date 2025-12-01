package cn.cug.sxy.ai.domain.armory.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @version 1.0
 * @Date 2025/11/25 14:39
 * @Description 装配命令 实体
 * @Author jerryhotton
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ArmoryCommandEntity {

    /**
     * 命令类型
     */
    private String commandType;
    /**
     * 命令索引（clientId、modelId、apiId...）
     */
    private List<String> commandIdList;

}
