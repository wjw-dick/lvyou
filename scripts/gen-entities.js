/**
 * 从 schema.sql 生成 MyBatis-Plus 实体与 Mapper。
 * 用法：node scripts/gen-entities.js
 */
const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const schema = fs.readFileSync(path.join(root, 'backend/src/main/resources/db/schema.sql'), 'utf8');
const javaRoot = path.join(root, 'backend/src/main/java/net/jxdg/travel');

const moduleOf = (table) => {
  if (table.startsWith('sys_')) return 'system';
  if (/^biz_(product|schedule|destination)/.test(table)) return 'product';
  if (/^biz_(order|payment|verify|refund)/.test(table)) return 'order';
  if (/^biz_(supplier|purchase)/.test(table)) return 'resource';
  if (table.startsWith('biz_guide_') || table === 'biz_guide') return 'guide';
  if (/^biz_(settlement|payment_plan|invoice|approval|commission|split|guarantee)/.test(table)) return 'finance';
  if (/^biz_(distributor|promotion)/.test(table)) return 'distribution';
  if (table.startsWith('biz_contract')) return 'contract';
  if (/^biz_(member|coupon|activity|points|wallet)/.test(table)) return 'member';
  if (/^biz_(scenic|ticket|traffic|parking)/.test(table) || table === 'biz_guide_audio') return 'scenic';
  if (/^biz_(ugc|custom|live|ai_)/.test(table)) return 'content';
  if (table.startsWith('biz_ota')) return 'ota';
  if (/^biz_(hotel|vehicle)/.test(table)) return 'supplierportal';
  if (table.startsWith('biz_insurance')) return 'insurance';
  if (/^biz_(currency|report_)/.test(table)) return 'report';
  return 'common';
};

const typeOf = (sqlType) => {
  const t = sqlType.toUpperCase();
  if (t.startsWith('BIGINT')) return 'Long';
  if (t.startsWith('TINYINT') || t.startsWith('INT') || t.startsWith('SMALLINT')) return 'Integer';
  if (t.startsWith('DECIMAL') || t.startsWith('NUMERIC')) return 'BigDecimal';
  if (t.startsWith('DATE') && !t.startsWith('DATETIME')) return 'LocalDate';
  if (t.startsWith('DATETIME') || t.startsWith('TIMESTAMP')) return 'LocalDateTime';
  return 'String';
};

const pascal = (s) => s.split('_').map(w => w.charAt(0).toUpperCase() + w.slice(1)).join('');
const camel = (s) => {
  const p = pascal(s);
  return p.charAt(0).toLowerCase() + p.slice(1);
};

// 提取建表块
const blocks = [];
const re = /CREATE TABLE `([a-z0-9_]+)` \(([\s\S]*?)\) ENGINE=/gi;
let m;
while ((m = re.exec(schema)) !== null) {
  blocks.push({ table: m[1], body: m[2] });
}

// 顶层逗号切分
function splitColumns(body) {
  const out = [];
  let depth = 0, cur = '';
  for (const ch of body) {
    if (ch === '(') depth++;
    if (ch === ')') depth--;
    if (ch === ',' && depth === 0) {
      out.push(cur.trim());
      cur = '';
    } else cur += ch;
  }
  if (cur.trim()) out.push(cur.trim());
  return out;
}

let count = 0;
for (const { table, body } of blocks) {
  const module = moduleOf(table);
  const entity = pascal(table);
  const cols = splitColumns(body).filter(l => /^`/.test(l));
  const fields = [];
  let hasLogicDelete = false;
  for (const col of cols) {
    const cm = col.match(/^`([a-z0-9_]+)`\s+([A-Za-z]+(?:\([^)]*\))?)([\s\S]*)$/);
    if (!cm) continue;
    const [, name, rawType, rest] = cm;
    const type = typeOf(rawType);
    const prop = camel(name);
    if (name === 'id') {
      fields.push('    @TableId(type = IdType.AUTO)\n    private Long id;');
      continue;
    }
    if (name === 'deleted') {
      hasLogicDelete = true;
      fields.push('    @TableLogic\n    private Integer deleted;');
      continue;
    }
    let ann = '';
    if (name === 'created_at') ann = '    @TableField(fill = FieldFill.INSERT)\n';
    if (name === 'updated_at') ann = '    @TableField(fill = FieldFill.INSERT_UPDATE)\n';
    fields.push(`${ann}    private ${type} ${prop};`);
  }
  const imports = new Set([
    'com.baomidou.mybatisplus.annotation.IdType',
    'com.baomidou.mybatisplus.annotation.TableId',
    'com.baomidou.mybatisplus.annotation.TableName',
    'lombok.Data'
  ]);
  if (fields.some(f => f.includes('TableField'))) {
    imports.add('com.baomidou.mybatisplus.annotation.TableField');
    imports.add('com.baomidou.mybatisplus.annotation.FieldFill');
  }
  if (hasLogicDelete) imports.add('com.baomidou.mybatisplus.annotation.TableLogic');
  const bodyTypes = fields.join('\n');
  if (bodyTypes.includes('BigDecimal')) imports.add('java.math.BigDecimal');
  if (/\bLocalDate\s+\w+;/.test(bodyTypes)) imports.add('java.time.LocalDate');
  if (/\bLocalDateTime\s+\w+;/.test(bodyTypes)) imports.add('java.time.LocalDateTime');

  const entityDir = path.join(javaRoot, 'modules', module, 'entity');
  const mapperDir = path.join(javaRoot, 'modules', module, 'mapper');
  fs.mkdirSync(entityDir, { recursive: true });
  fs.mkdirSync(mapperDir, { recursive: true });

  const java = `package net.jxdg.travel.modules.${module}.entity;

${[...imports].sort().map(i => `import ${i};`).join('\n')}

@Data
@TableName("${table}")
public class ${entity} {
${fields.join('\n\n')}
}
`;
  fs.writeFileSync(path.join(entityDir, `${entity}.java`), java, 'utf8');

  const mapper = `package net.jxdg.travel.modules.${module}.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.jxdg.travel.modules.${module}.entity.${entity};

public interface ${entity}Mapper extends BaseMapper<${entity}> {
}
`;
  fs.writeFileSync(path.join(mapperDir, `${entity}Mapper.java`), mapper, 'utf8');
  count++;
}

console.log(`generated ${count} entities + mappers`);
