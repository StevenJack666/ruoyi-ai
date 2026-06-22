package org.ruoyi.mapper.knowledge;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.ruoyi.domain.entity.knowledge.SessionUploadRecord;


@Mapper
public interface SessionUploadRecordMapper extends BaseMapper<SessionUploadRecord> {
    // insert/selectById/selectList 全部自动生成，不需要写任何SQL
}