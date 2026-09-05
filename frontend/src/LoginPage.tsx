import { Alert, Button, Card, Form, Input, Layout, Space, Typography } from 'antd';

const { Content } = Layout;
const { Paragraph, Title } = Typography;

type LoginPageProps = {
  submitting: boolean;
  errorMessage: string | null;
  onSubmit: (values: { username: string; password: string }) => Promise<void>;
};

function LoginPage({ submitting, errorMessage, onSubmit }: LoginPageProps) {
  return (
    <Layout style={{ minHeight: '100vh', background: '#f5f7fa' }}>
      <Content style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 24 }}>
        <Card style={{ width: '100%', maxWidth: 420 }}>
          <Space direction="vertical" size={20} style={{ width: '100%' }}>
            <div>
              <Title level={3} style={{ marginBottom: 8 }}>
                登录 Synapse PKB
              </Title>
              <Paragraph type="secondary" style={{ marginBottom: 0 }}>
                A1 阶段先落单用户 JWT 登录，登录后进入个人空间。
              </Paragraph>
            </div>

            {errorMessage ? <Alert type="error" showIcon message={errorMessage} /> : null}

            <Form layout="vertical" onFinish={onSubmit} autoComplete="off">
              <Form.Item
                label="用户名"
                name="username"
                rules={[{ required: true, message: '请输入用户名' }]}
              >
                <Input placeholder="请输入用户名" />
              </Form.Item>

              <Form.Item
                label="密码"
                name="password"
                rules={[{ required: true, message: '请输入密码' }]}
              >
                <Input.Password placeholder="请输入密码" />
              </Form.Item>

              <Button type="primary" htmlType="submit" loading={submitting} block>
                登录
              </Button>
            </Form>
          </Space>
        </Card>
      </Content>
    </Layout>
  );
}

export default LoginPage;
