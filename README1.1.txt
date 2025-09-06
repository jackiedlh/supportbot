详细部署及配置说明见共享文档
https://wlhmbop707.feishu.cn/wiki/EcirwBsRUiMasTkTs5mcj6WinFb?from=from_copylink

项目说明
1、customer-service-client
客服系统前端工程
此工程为demo工程主要提供和客服系统沟通能力

2、im-demo
即时通讯系统工程，提供客服系统和客服前端通讯能力
此工程为demo工程

3、question-classifier
请求分类模块，将请求按业务类型分类，发送到不同主体供各个Agent消费处理

4、assistant
Agent项目，处理需要调用工具处理的用户问题，支持根据请求类型动态装载提示词和MCP工具

5、knowledge-rag
知识库项目，通过知识检索回答用户政策类咨询问题

6、general-chat
智能问答项目，处理闲聊及不能明确分类的问题

7、product-service
商品服务，作为存量业务，通过Nacos和Higress配置提供mcp访问能力
此工程为demo工程，目的在于给Agent提供工具，接口的实现均为写死的数据

8、trade-service
交易服务，作为存量业务，通过Nacos和Higress配置提供mcp访问能力
此工程为demo工程，目的在于给Agent提供工具，接口的实现均为写死的数据

9、workorder-service
工单服务，作为存量业务，通过Nacos和Higress配置提供mcp访问能力
此工程为demo工程，目的在于给Agent提供工具，接口的实现均为写死的数据
