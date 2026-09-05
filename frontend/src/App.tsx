import { useEffect, useState } from 'react';
import { Alert, Button, Card, Descriptions, Layout, Space, Spin, Tag, Typography } from 'antd';
import LoginPage from './LoginPage';
import { ApiClientError } from './api';
import { fetchCurrentUser, hasStoredToken, login, logout, type CurrentUser } from './auth';

const { Content, Header } = Layout;
const { Paragraph, Text, Title } = Typography;

function App() {
  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null);
  const [initializing, setInitializing] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    if (!hasStoredToken()) {
      setInitializing(false);
      return;
    }

    fetchCurrentUser()
      .then((user) => {
        setCurrentUser(user);
      })
      .catch(() => {
        logout();
      })
      .finally(() => {
        setInitializing(false);
      });
  }, []);

  useEffect(() => {
    const handleUnauthorized = () => {
      logout();
      setCurrentUser(null);
      setErrorMessage('登录状态已失效，请重新登录。');
    };

    window.addEventListener('auth:unauthorized', handleUnauthorized);
    return () => {
      window.removeEventListener('auth:unauthorized', handleUnauthorized);
    };
  }, []);

  const handleLogin = async (values: { username: string; password: string }) => {
    setSubmitting(true);
    setErrorMessage(null);

    try {
      const user = await login(values.username, values.password);
      setCurrentUser(user);
    } catch (error) {
      logout();
      setCurrentUser(null);
      setErrorMessage(resolveErrorMessage(error));
    } finally {
      setSubmitting(false);
    }
  };

  const handleLogout = () => {
    logout();
    setCurrentUser(null);
    setErrorMessage(null);
  };

  if (initializing) {
    return (
      <Layout style={{ minHeight: '100vh', background: '#f5f7fa' }}>
        <Content style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <Space direction="vertical" align="center">
            <Spin size="large" />
            <Text type="secondary">正在恢复登录态...</Text>
          </Space>
        </Content>
      </Layout>
    );
  }

  if (!currentUser) {
    return <LoginPage submitting={submitting} errorMessage={errorMessage} onSubmit={handleLogin} />;
  }

  return (
    <Layout style={{ minHeight: '100vh', background: '#f5f7fa' }}>
      <Header style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', background: '#101828' }}>
        <Space size="middle">
          <Title level={3} style={{ margin: 0, color: '#fff' }}>
            Synapse PKB
          </Title>
          <Tag color="blue">A1 Ready</Tag>
        </Space>
        <Button onClick={handleLogout}>退出登录</Button>
      </Header>
      <Content style={{ padding: 32 }}>
        <Space direction="vertical" size={24} style={{ width: '100%' }}>
          <div>
            <Title level={2}>个人空间</Title>
            <Paragraph type="secondary" style={{ marginBottom: 0 }}>
              当前已完成 A1 的单用户认证闭环，后续业务查询会基于该用户上下文继续扩展。
            </Paragraph>
          </div>

          <Card title="当前登录用户">
            <Descriptions column={1} bordered>
              <Descriptions.Item label="用户 ID">{currentUser.id}</Descriptions.Item>
              <Descriptions.Item label="用户名">{currentUser.username}</Descriptions.Item>
              <Descriptions.Item label="显示名称">{currentUser.displayName}</Descriptions.Item>
              <Descriptions.Item label="空间标识">{currentUser.spaceKey}</Descriptions.Item>
            </Descriptions>
          </Card>

          <Alert
            type="info"
            showIcon
            message="数据库基线已预留"
            description="A1 已引入用户表和固定 user 上下文，后续 entries、relations 等业务表落地时将按该基线补齐 user_id 字段与索引。"
          />

          <Card title="下一步衔接">
            <Space direction="vertical" size={8}>
              <Text>1. 在 A2/A5 阶段为业务表补齐 user_id 字段与索引。</Text>
              <Text>2. 在统一录入与 CRUD 模块里接入当前用户过滤。</Text>
              <Text>3. 后续可在不推翻 A1 的前提下扩展为多用户模式。</Text>
            </Space>
          </Card>
        </Space>
      </Content>
    </Layout>
  );
}

function resolveErrorMessage(error: unknown) {
  if (error instanceof ApiClientError) {
    if (error.status === 401) {
      return '用户名或密码不正确。';
    }

    if (error.message.trim()) {
      return error.message;
    }
  }

  return '登录失败，请稍后重试。';
}

export default App;
