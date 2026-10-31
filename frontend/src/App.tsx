import { useEffect, useState } from 'react';
import { Button, Card, Descriptions, Layout, Space, Spin, Tabs, Tag, Typography } from 'antd';
import LoginPage from './LoginPage';
import TagManager from './TagManager';
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
      <Header style={{ display: 'flex', flexWrap: 'wrap', gap: 12, padding: '12px 16px', height: 'auto',
        minHeight: 64, alignItems: 'center', justifyContent: 'space-between', background: '#101828' }}>
        <Space size="middle" wrap>
          <Title level={3} style={{ margin: 0, color: '#fff' }}>
            Synapse PKB
          </Title>
          <Tag color="blue">个人知识库</Tag>
        </Space>
        <Button onClick={handleLogout}>退出登录</Button>
      </Header>
      <Content style={{ padding: 16 }}>
        <Space direction="vertical" size={24} style={{ width: '100%' }}>
          <div>
            <Title level={2}>个人空间</Title>
            <Paragraph type="secondary" style={{ marginBottom: 0 }}>
              管理自己的分类与标签，逐步整理个人知识。
            </Paragraph>
          </div>

          <Tabs defaultActiveKey="tags" items={[
            { key: 'tags', label: '分类与标签', children: <TagManager /> },
            { key: 'account', label: '当前用户', children: <Card title="当前登录用户">
            <Descriptions column={1} bordered>
              <Descriptions.Item label="用户 ID">{currentUser.id}</Descriptions.Item>
              <Descriptions.Item label="用户名">{currentUser.username}</Descriptions.Item>
              <Descriptions.Item label="显示名称">{currentUser.displayName}</Descriptions.Item>
              <Descriptions.Item label="空间标识">{currentUser.spaceKey}</Descriptions.Item>
            </Descriptions>
          </Card> }
          ]} />
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
