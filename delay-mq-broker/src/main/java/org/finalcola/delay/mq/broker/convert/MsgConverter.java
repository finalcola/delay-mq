package org.finalcola.delay.mq.broker.convert;

import com.google.common.base.Joiner;
import org.finalcola.delay.mq.common.proto.DelayMsg;
import org.finalcola.delay.mq.common.proto.MetaData;
import org.finalcola.delay.mq.common.proto.MsgDataWrapper;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author: finalcola
 * @date: 2023/3/18 13:03
 */
public class MsgConverter {

    public static ByteBuffer buildKey(@Nonnull DelayMsg msg) {
        MetaData metaData = msg.getMetaData();
        String keyStr = Joiner.on("|")
                .join(metaData.getDelayMills(), metaData.getTopic(), metaData.getMsgId());
        return ByteBuffer.wrap(keyStr.getBytes(UTF_8));
    }

    public static DelayMsg buildDelayMsg(String msgId, MsgDataWrapper wrapper) {
        MetaData metaData = MetaData.newBuilder()
                .setMsgId(msgId)
                .setMsgKey(wrapper.getMsgKey())
                .setCreateTimestamp(wrapper.getCreateTime())
                .setDelayMills(wrapper.getDelayMills())
                .setTopic(wrapper.getTopic())
                .setTags(wrapper.getTags())
                .setPartitionKey(wrapper.getPartitionKey())
                .build();
        return DelayMsg.newBuilder()
                .setMetaData(metaData)
                .setBody(wrapper.getData())
                .build();
    }
}
