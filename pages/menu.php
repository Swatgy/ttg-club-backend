<nav id="navbar" class="navbar">
    <a class="logo" href="index.php"></a>
    <ul class="navbar_nav">
        <li id="dropdown_character" class="nav_item dropdown_item">
            <a class="nav_link" href="#">
                <div class="nav_icon icon_character"></div>
                <span>Персонаж</span>
                <div class="nav_icon right_icon folded"></div>
            </a>
        
            <ul class="d_menu">
					<li><a href="classes.php">Классы</a></li>
					<li><a href="#">Рассы</a></li>
					<li><a href="#">Опции и особенности</a></li>
					<li><a href="#">Предыстории</a></li>
					<li><a href="#">Черты</a></li>
            </ul>        
        </li>

        <li class="nav_item">
            <a class="nav_link" href="#"><div class="nav_icon icon_spells"></div><span>Заклинания</span></a>
        </li>

        <li id="dropdown_inventory" class="nav_item dropdown_item">
            <a class="nav_link" href="#">
                <div class="nav_icon icon_inventory"></div>
                <span>Инвентарь</span>
                <div class="nav_icon right_icon folded"></div>
            </a>
            <ul class="d_menu">
					<li><a href="#">Оружие</a></li>
					<li><a href="#">Доспехи</a></li>
					<li><a href="#">Снаряжение</a></li>
            </ul>
        </li>

        <li onclick="switchTheme()" class="nav_item">
            <a class="nav_link" href="#"><div class="nav_icon icon_dark_mod"></div><span>Dark Mod</span></a>
        </li>
    </ul>
    <div class="copyright_block">
        <p class="caption_text">Материалы на данном сайте приведены исключительно в справочных и ознакомительных целях и не заменяют необходимость покупки официальных материалов по игре. Весь графический материал и система Dungeons&Dragons является собственностью корпорации Wizards of the Coast.</p>
    </div>
</nav>