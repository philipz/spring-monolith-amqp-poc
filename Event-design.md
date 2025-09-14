好的！我把 Spring Modulith 的「應用程式事件（Application Events）」機制，以及用 spring-modulith-events-amqp 將事件對接 RabbitMQ 的完整做法拆成兩個方向來說明，並補上可直接複製運行的程式碼與參考範例：
	1.	內部模組事件 →（外部化）→ RabbitMQ
	2.	RabbitMQ 訊息 →（轉為應用事件）→ 觸發後續處理

下面每段都附上權威文件或實作參考。

⸻

1) 應用事件（Application Events）與 @ApplicationModuleListener

在 Modulith 中，建議模組之間用「發布/訂閱事件」解耦；預設推薦的監聽寫法是 @ApplicationModuleListener，它等同於
@Async + @Transactional(REQUIRES_NEW) + @TransactionalEventListener：主交易提交後非同步處理，並各自包在新交易，失敗可依事件登錄表（Event Publication Registry）重送/管理。 ￼

最小示例：

// 事件（可放在發佈模組的 domain/events 套件）
public record OrderCompleted(UUID orderId) {}

// 發佈端（在主交易中 publish）
@Service
@RequiredArgsConstructor
class OrderManagement {
  private final ApplicationEventPublisher events;

  @Transactional
  public void complete(Order order) {
    // ...聚合狀態轉移
    events.publishEvent(new OrderCompleted(order.getId()));
  }
}

// 訂閱端（跨模組用事件整合）
@Component
class InventoryManagement {
  @ApplicationModuleListener
  void on(OrderCompleted event) {
    // 後續處理（在新交易中非同步執行）
  }
}

上述模式與事件登錄表的運作、清理策略（UPDATE/DELETE/ARCHIVE）等細節見官方文件。 ￼

⸻

2) 外部化到 RabbitMQ：spring-modulith-events-amqp

目標：把內部發佈的應用事件，經由 Modulith 的「事件外部化」機制，送到 RabbitMQ（AMQP）。做法是：
	•	引入 spring-modulith-events-amqp（以及 Spring AMQP/Rabbit 依賴）
	•	用 @Externalized 標示哪些事件要送出，以及目標（通常是 exchange）與 routing key
	•	（必要時）宣告交換器/佇列/繫結

2.1 依賴

<!-- 事件外部化：AMQP -->
<dependency>
  <groupId>org.springframework.modulith</groupId>
  <artifactId>spring-modulith-events-amqp</artifactId>
  <version>1.4.3</version>
</dependency>

<!-- Spring AMQP / Rabbit -->
<dependency>
  <groupId>org.springframework.amqp</groupId>
  <artifactId>spring-rabbit</artifactId>
</dependency>

<!-- 事件序列化（預設 Jackson） -->
<dependency>
  <groupId>org.springframework.modulith</groupId>
  <artifactId>spring-modulith-events-jackson</artifactId>
  <version>1.4.3</version>
</dependency>

Modulith 官方說明 AMQP 外部化、需要 Spring Rabbit，並提供自動設定 RabbitEventExternalizerConfiguration。 ￼

2.2 標註外部化與路由

對要外部化的事件，用 @Externalized 指定「目標::routingKey」。在 AMQP 情境下，目標通常對應 exchange 名稱，routing key 會用於 AMQP 的路由。 ￼

import org.springframework.modulith.events.Externalized;

// 送往 exchange = "domain.events"，routingKey = "order.completed"
@Externalized("domain.events::order.completed")
public record OrderCompleted(UUID orderId) {}

假如 routing key 需要動態（SpEL），也可寫成：

@Externalized("domain.events::#{#this.orderId()}")
public record OrderCompleted(UUID orderId) {}

（官方文件示例以 target::#{SpEL} 形式動態計算 routing key） ￼

2.3 宣告 Exchange / Queue / Binding（Spring AMQP）

使用 Spring AMQP 宣告一個 direct/topic exchange 與 queue 的繫結，routing key 要與上面一致：

@Configuration
class RabbitTopologyConfig {

  public static final String EXCHANGE = "domain.events";
  public static final String QUEUE    = "inventory.order.completed.q";
  public static final String ROUTING  = "order.completed";

  @Bean
  DirectExchange domainEventsExchange() {
    return new DirectExchange(EXCHANGE, true, false);
  }

  @Bean
  Queue inventoryQueue() {
    return QueueBuilder.durable(QUEUE).build();
  }

  @Bean
  Binding bindInventoryQueue(Queue inventoryQueue, DirectExchange domainEventsExchange) {
    return BindingBuilder.bind(inventoryQueue).to(domainEventsExchange).with(ROUTING);
  }
}

Spring AMQP 對交換器/佇列/繫結與 routing key 的觀念與宣告方式可參考官方與教學文。 ￼

小提醒：@ApplicationModuleListener 只有在主交易成功提交時才會被視為完成；外部化同樣基於「交易提交」後進行，避免把外部 I/O 放進主交易造成風險與延遲。 ￼

⸻

3) 從 RabbitMQ「訂閱佇列」→ 轉成應用事件再觸發處理

spring-modulith-events-amqp 的職責是外部化事件到 AMQP；要從 AMQP 訂閱再轉回內部流程，做法是用 Spring AMQP 的 @RabbitListener 當「防腐層/適配器」，把外部訊息反序列化後轉成內部應用事件發布（或直接呼叫應用服務）。

@Component
@RequiredArgsConstructor
class InboundAmqpAdapter {

  private final ApplicationEventPublisher events;

  // 訂閱上面宣告的 queue
  @RabbitListener(queues = RabbitTopologyConfig.QUEUE)
  void onMessage(OrderCompletedMessage dto) {
    // 轉為內部應用事件（或直接呼叫應用服務）
    events.publishEvent(new OrderCompleted(dto.orderId()));
  }
}

// 假設外部訊息的結構（可用 Jackson）
record OrderCompletedMessage(UUID orderId) {}

Spring 指南「Messaging with RabbitMQ」與 Baeldung 都有 @RabbitListener、拓樸宣告與 YAML 連線設定的完整示例可對照。 ￼

⸻

4) application.yaml 範例（連線設定）

spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest

  modulith:
    events:
      externalization:
        enabled: true  # 預設即為 true

spring-modulith-events-amqp 會自動裝配外部化元件；AMQP 發送則透過 Spring Rabbit 的 RabbitTemplate 與我們在 2.3 宣告的 exchange/routing。 ￼

⸻

5) 端到端流程（兩條路徑）
	•	內→外：OrderManagement.complete() 發佈 OrderCompleted → 事件被登錄，交易提交後 → Modulith 判斷事件被 @Externalized → 透過 AMQP 外部化到 exchange=domain.events、routingKey=order.completed。 ￼
	•	外→內：RabbitMQ 上 inventory.order.completed.q 有新訊息 → @RabbitListener 收到並轉為 OrderCompleted 應用事件 → 由各模組用 @ApplicationModuleListener 非同步處理。 ￼

⸻

6) 參考範例與延伸閱讀
	•	官方文件：Working with Application Events（包含 @ApplicationModuleListener、事件登錄表、外部化與 AMQP 支援） ￼
	•	Javadoc：ApplicationModuleListener 說明（為何是預設整合方式） ￼
	•	AMQP 外部化自動設定：org.springframework.modulith.events.amqp 套件與 RabbitEventExternalizerConfiguration  ￼
	•	Spring 指南：Messaging with RabbitMQ（@RabbitListener/拓樸宣告） ￼
	•	RabbitMQ 路由概念（exchange、routing key、bindings） ￼
	•	跨模組事件示例/教學（Modulith 入門與事件） ￼
	•	範例專案（完整 Modulith 應用，可對照事件與模組切分；如需自行加 AMQP 外部化很容易套入） ￼

⸻

7) 常見坑位 & 建議
	1.	交易時機：@ApplicationModuleListener 屬於 TransactionalEventListener，只有主交易 commit 才會觸發；若你在非交易或尚未 commit 的地方發佈事件，監聽器不會動作。 ￼
	2.	事件登錄清理：預設完成的事件會保留在表中，請設定清理策略（spring.modulith.events.completion-mode=DELETE 或定期清理）。 ￼
	3.	AMQP 拓樸一致性：@Externalized 的 target::routingKey 要與 RabbitMQ 的 exchange/繫結一致；否則訊息會被丟棄。 ￼
	4.	雙向整合分工：spring-modulith-events-amqp 專注「送出」；「訂閱」請用 @RabbitListener 寫 inbound adapter，再轉 Application Event 或呼叫應用服務。 ￼
	5.	設計語意：事件命名用「已發生的事」（如 OrderCompleted），不要用資料物件名（如 ProductEvent）；事件屬於發佈它的模組。 ￼
