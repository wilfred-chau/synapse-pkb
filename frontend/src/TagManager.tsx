import { useCallback, useEffect, useRef, useState } from 'react';
import { Alert, Button, Card, Empty, Input, List, Modal, Select, Space, Spin, Tag, Typography } from 'antd';
import { ApiClientError, getApi, patchApi, postApi } from './api';

type Category = { code: string; name: string };
type ManagedTag = { id: number; name: string; createdAt: string; updatedAt: string };
type Dialog = { kind: 'create' } | { kind: 'rename' | 'merge'; tag: ManagedTag };

function errorText(error: unknown) {
  if (error instanceof ApiClientError) {
    const messages: Record<string, string> = {
      TAG_INVALID_NAME: '名称规范化后须为 1–32 个字符，不能包含控制字符或隐藏分隔符。',
      TAG_NAME_CONFLICT: '已有同名标签，请换一个名称；需要合并时请使用合并操作。',
      TAG_NOT_FOUND: '标签不存在或已被合并，请关闭弹窗并刷新列表。',
      TAG_OPERATION_CONFLICT: '标签正在被修改，请刷新后重试。',
      TAG_MERGE_SAME_TAG: '不能合并到自身。',
      AUTHENTICATION_REQUIRED: '请重新登录。'
    };
    return messages[error.code ?? ''] ?? '操作失败，请稍后重试。';
  }
  return '网络请求失败，请检查连接后重试。';
}

export default function TagManager() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [tags, setTags] = useState<ManagedTag[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string>();
  const [notice, setNotice] = useState<string>();
  const [search, setSearch] = useState('');
  const [dialog, setDialog] = useState<Dialog>();
  const [name, setName] = useState('');
  const [targetId, setTargetId] = useState<number>();
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState<string>();
  const generation = useRef(0);
  const inFlight = useRef(false);

  const reload = useCallback(async () => {
    const current = ++generation.current;
    setLoading(true);
    setLoadError(undefined);
    try {
      const [nextCategories, nextTags] = await Promise.all([
        getApi<Category[]>('/categories'), getApi<ManagedTag[]>('/tags')
      ]);
      if (current === generation.current) {
        setCategories(nextCategories);
        setTags(nextTags);
      }
    } catch (error) {
      if (current === generation.current) setLoadError(errorText(error));
    } finally {
      if (current === generation.current) setLoading(false);
    }
  }, []);

  useEffect(() => {
    void reload();
    return () => { generation.current++; };
  }, [reload]);

  const openDialog = (next: Dialog) => {
    setDialog(next);
    setName(next.kind === 'rename' ? next.tag.name : '');
    setTargetId(undefined);
    setFormError(undefined);
    setNotice(undefined);
  };

  const submit = async () => {
    if (!dialog || inFlight.current) return;
    if (dialog.kind === 'merge' ? targetId === undefined : !name.trim()) {
      setFormError(dialog.kind === 'merge' ? '请选择目标标签。' : '请输入标签名称。');
      return;
    }
    inFlight.current = true;
    setSaving(true);
    setFormError(undefined);
    try {
      let resultMessage = '';
      if (dialog.kind === 'create') {
        const tag = await postApi<ManagedTag>('/tags', { name });
        resultMessage = `已创建标签「${tag.name}」。`;
      } else if (dialog.kind === 'rename') {
        const result = await patchApi<{ tag: ManagedTag; changed: boolean }>(`/tags/${dialog.tag.id}`, { name });
        resultMessage = result.changed ? `已重命名为「${result.tag.name}」。` : '名称未变化。';
      } else {
        const result = await postApi<{ targetTag: ManagedTag }>(`/tags/${dialog.tag.id}/merge`, { targetTagId: targetId });
        resultMessage = `已合并到「${result.targetTag.name}」，源标签已移除。`;
      }
      setDialog(undefined);
      setNotice(resultMessage);
      await reload();
    } catch (error) {
      setFormError(`${errorText(error)} 若请求超时，先刷新核对结果再重试。`);
    } finally {
      setSaving(false);
      inFlight.current = false;
    }
  };

  const visibleTags = tags.filter(tag => tag.name.toLowerCase().includes(search.toLowerCase()));
  const target = tags.find(tag => tag.id === targetId);

  return (
    <Space direction="vertical" size={20} style={{ width: '100%' }}>
      <Card title="一级分类">
        <Space wrap>{categories.map(category => <Tag key={category.code}>{category.name}</Tag>)}</Space>
        <Typography.Paragraph type="secondary" style={{ marginTop: 12, marginBottom: 0 }}>
          五类固定，用于条目分类。你的标签跨分类共享，不属于某个分类。
        </Typography.Paragraph>
      </Card>
      <Alert type="info" showIcon message="当前为手动标签管理"
        description="条目使用统计、条目标签关联及 AI 候选推荐尚未接入。本页合并只处理标签字典。" />
      {notice && <Alert type="success" showIcon message={notice} />}
      {loadError && <Alert type="error" showIcon message="列表加载失败" description={loadError}
        action={<Button onClick={() => void reload()}>重试</Button>} />}
      <Card title="我的标签" extra={<Button onClick={() => void reload()} disabled={loading || saving}>刷新</Button>}>
        <Space wrap style={{ marginBottom: 16 }}>
          <Input aria-label="搜索标签" placeholder="搜索标签" allowClear value={search}
            onChange={event => setSearch(event.target.value)} />
          <Button type="primary" disabled={loading || saving || !!loadError} onClick={() => openDialog({ kind: 'create' })}>
            创建标签
          </Button>
        </Space>
        <Spin spinning={loading}>
          <List dataSource={visibleTags} locale={{ emptyText: <Empty description={search ? '没有匹配的标签' : '还没有标签'} /> }}
            renderItem={tag => (
              <List.Item actions={[
                <Button key="rename" type="link" disabled={loading || saving || !!loadError}
                  onClick={() => openDialog({ kind: 'rename', tag })}>重命名</Button>,
                <Button key="merge" type="link" disabled={tags.length < 2 || loading || saving || !!loadError}
                  onClick={() => openDialog({ kind: 'merge', tag })}>合并</Button>
              ]}>
                <Typography.Text style={{ overflowWrap: 'anywhere' }}>{tag.name}</Typography.Text>
              </List.Item>
            )} />
        </Spin>
      </Card>
      <Modal open={!!dialog} title={dialog?.kind === 'create' ? '创建标签' : dialog?.kind === 'rename' ? '重命名标签' : '合并标签'}
        okText={dialog?.kind === 'merge' ? '确认合并' : '保存'} cancelText="取消" confirmLoading={saving}
        closable={!saving} maskClosable={!saving} keyboard={!saving} cancelButtonProps={{ disabled: saving }}
        onCancel={() => { if (!saving) setDialog(undefined); }} onOk={() => void submit()}>
        <Space direction="vertical" size={16} style={{ width: '100%', marginTop: 12 }}>
          {dialog?.kind === 'merge' ? <>
            <Typography.Text>源标签：{dialog.tag.name}</Typography.Text>
            <Select aria-label="目标标签" placeholder="选择保留的目标标签" style={{ width: '100%' }}
              value={targetId} onChange={setTargetId} disabled={saving} showSearch optionFilterProp="label"
              options={tags.filter(tag => tag.id !== dialog.tag.id).map(tag => ({ value: tag.id, label: tag.name }))} />
            <Alert type="warning" showIcon message={target ? `「${dialog.tag.name}」将合并到「${target.name}」` : '合并不可撤销'}
              description="保留目标标签，删除源标签；源名称可再次创建。当前没有条目关联迁移。" />
          </> : <>
            <Input aria-label="标签名称" placeholder="标签名称" value={name} disabled={saving}
              onChange={event => setName(event.target.value)} onPressEnter={() => void submit()} />
            <Typography.Text type="secondary">1–32 个字符；多余空白将合并，英文字母大小写不影响判重。</Typography.Text>
          </>}
          {formError && <Alert type="error" showIcon message={formError} />}
        </Space>
      </Modal>
    </Space>
  );
}
