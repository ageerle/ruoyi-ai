-- Keep the original permissions while moving knowledge files and fragments under the library.
UPDATE sys_menu AS child
JOIN sys_menu AS library
  ON library.menu_id = 2006681261898813441
 AND library.component = 'knowledge/info/index'
SET child.parent_id = library.menu_id,
    child.visible = '1'
WHERE child.menu_id IN (2099010100000000010, 2099010100000000020)
  AND child.component IN ('knowledge/attach/index', 'knowledge/fragment/index');
