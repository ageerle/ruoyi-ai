-- A document's fragments are managed from its attachment.
START TRANSACTION;

UPDATE sys_menu AS attachment
JOIN sys_menu AS library
  ON library.menu_id = 2006681261898813441
 AND library.component = 'knowledge/info/index'
SET attachment.parent_id = library.menu_id,
    attachment.order_num = 6,
    attachment.visible = '1'
WHERE attachment.menu_id = 2099010100000000010
  AND attachment.component = 'knowledge/attach/index';

UPDATE sys_menu AS fragment
JOIN sys_menu AS attachment
  ON attachment.menu_id = 2099010100000000010
 AND attachment.component = 'knowledge/attach/index'
 AND attachment.parent_id = 2006681261898813441
SET fragment.parent_id = attachment.menu_id,
    fragment.order_num = 6,
    fragment.visible = '1'
WHERE fragment.menu_id = 2099010100000000020
  AND fragment.component = 'knowledge/fragment/index';

COMMIT;
