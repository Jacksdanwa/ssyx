package com.atme.ssyx.order.receiver;

import com.atme.ssyx.common.constant.MqConst;
import com.atme.ssyx.order.service.OrderInfoService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;

@Component
public class OrderReceiver {

    @Autowired
    private OrderInfoService orderInfoService;


    /*
    *
    * message
作用：message 是从消息队列（如 RabbitMQ 或 Kafka）中接收到的消息对象。它包含了消息的各种信息，比如消息内容、属性、路由键等。

用途：在这段代码中，message 是用来获取消息的属性（比如 DeliveryTag）并在处理完消息后确认该消息已经被消费。消息对象包含了被消费者（这个方法）接收到的具体消息内容以及相关的元数据。

channel
作用：channel 是消息通道对象，通常用于与消息中间件（如 RabbitMQ）进行交互。它提供了基本的消息队列操作，如确认消息、发送消息、关闭通道等。

用途：在这里，channel 用于确认消息的消费状态（即调用 basicAck）。这也是确保消息在队列中只被处理一次的一种机制。*/


    //订单支付成功，更新状态，扣减库存
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = MqConst.QUEUE_ORDER_PAY,durable = "true"),exchange = @Exchange(value = MqConst.EXCHANGE_PAY_DIRECT),key = {MqConst.ROUTING_PAY_SUCCESS}))
    public void orderPay(String orderNo, Message message, Channel channel) throws IOException {
        if (!StringUtils.isEmpty(orderNo)) {
            orderInfoService.orderPay(orderNo);
        }
        /*
        * 参数 1 (message.getMessageProperties().getDeliveryTag())：这个参数是消息的 DeliveryTag，是一个唯一的标识符，用于标记该消息的编号。在确认消息时，DeliveryTag 用来指定要确认的具体消息。
        参数 2 (false)：第二个参数是一个布尔值，表示是否确认多个消息。如果是 true，则表示确认所有从 DeliveryTag 之前的所有消息；如果是 false，则只确认当前指定的这条消息。*/
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);


    }


}
