import { Card, Col, Layout, Row, Space, Tag, Typography } from 'antd';

const { Content, Header } = Layout;
const { Paragraph, Title, Text } = Typography;

const cards = [
  {
    title: 'Backend',
    description: 'Spring Boot 3 skeleton is prepared for future module-based development.',
    tag: 'Java 17'
  },
  {
    title: 'Frontend',
    description: 'React 18 + TypeScript + Ant Design shell is ready for incremental pages.',
    tag: 'Vite'
  },
  {
    title: 'Current Scope',
    description: 'Only project scaffolding is in place. No business modules or APIs are implemented yet.',
    tag: 'Skeleton'
  }
];

function App() {
  return (
    <Layout style={{ minHeight: '100vh', background: '#f5f7fa' }}>
      <Header style={{ display: 'flex', alignItems: 'center', background: '#101828' }}>
        <Space size="middle">
          <Title level={3} style={{ margin: 0, color: '#fff' }}>
            Synapse PKB
          </Title>
          <Tag color="blue">Dev Skeleton</Tag>
        </Space>
      </Header>
      <Content style={{ padding: 32 }}>
        <Space direction="vertical" size={24} style={{ width: '100%' }}>
          <div>
            <Title level={2}>项目骨架已初始化</Title>
            <Paragraph type="secondary">
              当前阶段只落基础设施，不实现任何业务需求。后续可以在这个壳子上继续逐模块推进。
            </Paragraph>
          </div>

          <Row gutter={[16, 16]}>
            {cards.map((card) => (
              <Col xs={24} md={8} key={card.title}>
                <Card title={card.title} extra={<Tag>{card.tag}</Tag>} style={{ height: '100%' }}>
                  <Paragraph style={{ marginBottom: 0 }}>{card.description}</Paragraph>
                </Card>
              </Col>
            ))}
          </Row>

          <Card title="下一步建议">
            <Space direction="vertical" size={8}>
              <Text>1. 在 backend 中补充分层目录与统一返回结构。</Text>
              <Text>2. 在 frontend 中补充路由、布局壳和 API 基础封装。</Text>
              <Text>3. 再进入模块 A 的最小闭环实现。</Text>
            </Space>
          </Card>
        </Space>
      </Content>
    </Layout>
  );
}

export default App;
